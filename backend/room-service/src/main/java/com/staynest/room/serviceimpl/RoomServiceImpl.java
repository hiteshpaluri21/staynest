package com.staynest.room.serviceimpl;

import com.staynest.room.audit.AuditRecorder;
import com.staynest.room.dto.RoomRequest;
import com.staynest.room.dto.RoomResponse;
import com.staynest.room.entity.Room;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RoomStatus;
import com.staynest.room.exception.BadRequestException;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.exception.ServiceUnavailableException;
import com.staynest.room.repository.RoomRepository;
import com.staynest.room.repository.RoomTypeRepository;
import com.staynest.room.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.staynest.room.client.ReservationServiceClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class RoomServiceImpl implements RoomService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "ROOM";

    @Autowired
    private AuditRecorder auditRecorder;

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional
    public RoomResponse addRoom(RoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId()));

        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new BadRequestException("A room with number " + request.getRoomNumber() + " already exists");
        }

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);

        Room saved = roomRepository.save(room);
        log.info("Room created: {}", saved.getRoomId());
        auditRecorder.record("CREATE", ENTITY, saved.getRoomId());
        return mapToResponse(saved);
    }

    @Override
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public RoomResponse getRoomById(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        return mapToResponse(room);
    }

    @Autowired(required = false)
    private ReservationServiceClient reservationServiceClient;

    @Override
    public List<RoomResponse> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /** Statuses that take a room out of the bookable inventory altogether. */
    private static final Set<RoomStatus> OUT_OF_SERVICE =
            EnumSet.of(RoomStatus.MAINTENANCE, RoomStatus.BLOCKED);

    /**
     * Rooms that are not already committed for the given dates.
     *
     * Reads as four steps: take the bookable inventory, count what is booked per room type over
     * the window, subtract, and map. Each step is a method below.
     */
    @Override
    public List<RoomResponse> getAvailableRooms(String checkIn, String checkOut) {
        // With no dates there is no availability question to answer — report what is free
        // right now, which is exactly what the AVAILABLE status means.
        if (checkIn == null || checkOut == null) {
            return roomRepository.findByStatus(RoomStatus.AVAILABLE).stream()
                    .map(this::mapToResponse).collect(Collectors.toList());
        }

        LocalDate searchIn = LocalDate.parse(checkIn);
        LocalDate searchOut = LocalDate.parse(checkOut);

        Map<Integer, Long> bookedCounts = countBookedByRoomType(fetchReservations(), searchIn, searchOut);

        return takeUnbookedRooms(bookableInventory(), bookedCounts).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Every room that could take a guest on some date — that is, everything except the rooms
     * withdrawn from service. Deliberately includes rooms that are OCCUPIED or CLEANING today:
     * a room's status describes *now*, while whether it is free for the requested window is
     * decided by the reservations, below.
     *
     * This used to start from status AVAILABLE, which counted a check-in twice. The room left
     * the pool as it turned OCCUPIED, and its reservation — now CHECKEDIN — still counted as
     * booked, so the advertised count dropped by one when the room was reserved and again when
     * the guest arrived. It also hid a room occupied today from a search for next month.
     */
    private List<Room> bookableInventory() {
        return roomRepository.findAll().stream()
                .filter(room -> !OUT_OF_SERVICE.contains(room.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Every reservation known to reservation-service.
     *
     * Fails closed: anything that stops the cross-check throws rather than returning an
     * unfiltered list, which would advertise already-booked rooms and invite double bookings —
     * exactly what happened while the JWT was not being forwarded.
     */
    private List<?> fetchReservations() {
        if (reservationServiceClient == null) {
            throw new ServiceUnavailableException(
                    "Cannot verify room availability: reservation-service client is unavailable.");
        }
        try {
            var resResponse = reservationServiceClient.getAllReservations(null);
            if (resResponse == null || !(resResponse.getData() instanceof List)) {
                throw new ServiceUnavailableException(
                        "Cannot verify room availability: reservation-service returned no data.");
            }
            return (List<?>) resResponse.getData();
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Availability cross-check against reservation-service failed", e);
            throw new ServiceUnavailableException(
                    "Cannot verify room availability right now. Please try again in a moment.");
        }
    }

    /** How many live reservations of each room type overlap the requested window. */
    private Map<Integer, Long> countBookedByRoomType(List<?> reservations, LocalDate searchIn, LocalDate searchOut) {
        return reservations.stream()
                .filter(Map.class::isInstance)
                .map(obj -> (Map<?, ?>) obj)
                .filter(res -> holdsARoom(res) && overlaps(res, searchIn, searchOut))
                .filter(res -> res.get("roomTypeId") != null)
                .collect(Collectors.groupingBy(
                        res -> Integer.parseInt(res.get("roomTypeId").toString()),
                        Collectors.counting()));
    }

    /**
     * Only confirmed and checked-in reservations occupy a room; cancelled and checked-out ones
     * free it. Both live states count exactly once, which is what keeps the number steady as a
     * booking moves from CONFIRMED to CHECKEDIN.
     */
    private boolean holdsARoom(Map<?, ?> reservation) {
        Object status = reservation.get("status");
        if (status == null) return false;
        String st = status.toString();
        return "CONFIRMED".equalsIgnoreCase(st) || "CHECKEDIN".equalsIgnoreCase(st);
    }

    /**
     * Half-open overlap: a stay ending on the day another begins does not clash, because the
     * room is turned over that morning.
     */
    private boolean overlaps(Map<?, ?> reservation, LocalDate searchIn, LocalDate searchOut) {
        Object inObj = reservation.get("checkInDate");
        Object outObj = reservation.get("checkOutDate");
        if (inObj == null || outObj == null) return false;
        LocalDate resIn = LocalDate.parse(inObj.toString());
        LocalDate resOut = LocalDate.parse(outObj.toString());
        return resIn.isBefore(searchOut) && resOut.isAfter(searchIn);
    }

    /**
     * Per room type, drops as many rooms as are already booked. Which specific rooms remain does
     * not matter — only how many — since a guest books a type and is assigned a room at check-in.
     */
    private List<Room> takeUnbookedRooms(List<Room> inventory, Map<Integer, Long> bookedCounts) {
        Map<Integer, List<Room>> roomsByType = inventory.stream()
                .collect(Collectors.groupingBy(r -> r.getRoomType().getRoomTypeId()));

        List<Room> remaining = new ArrayList<>();
        roomsByType.forEach((typeId, typeRooms) -> {
            long booked = bookedCounts.getOrDefault(typeId, 0L);
            // Clamped to the list size, so the sublist bound is always valid.
            int keep = Math.max(0, (int) (typeRooms.size() - booked));
            remaining.addAll(typeRooms.subList(0, keep));
        });
        return remaining;
    }

    @Override
    public List<RoomResponse> getRoomsByType(Integer roomTypeId) {
        return roomRepository.findByRoomType_RoomTypeId(roomTypeId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomResponse updateRoomStatus(Integer id, RoomStatus status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setStatus(status);
        Room updated = roomRepository.save(room);
        log.info("Room {} status updated to {}", id, status);
        auditRecorder.record("UPDATE_STATUS", ENTITY, id);
        return mapToResponse(updated);
    }

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setRoomId(room.getRoomId());
        response.setRoomNumber(room.getRoomNumber());
        response.setFloor(room.getFloor());
        response.setRoomTypeId(room.getRoomType().getRoomTypeId());
        response.setRoomTypeName(room.getRoomType().getName().name());
        response.setStatus(room.getStatus());
        return response;
    }
}