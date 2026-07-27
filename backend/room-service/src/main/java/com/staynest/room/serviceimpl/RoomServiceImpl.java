package com.staynest.room.serviceimpl;

import com.staynest.room.dto.RoomRequest;
import com.staynest.room.dto.RoomResponse;
import com.staynest.room.entity.Room;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RoomStatus;
import com.staynest.room.exception.BadRequestException;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.repository.RoomRepository;
import com.staynest.room.repository.RoomTypeRepository;
import com.staynest.room.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Override
    public RoomResponse addRoom(RoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId()));

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);

        Room saved = roomRepository.save(room);
        log.info("Room created: {}", saved.getRoomId());
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
    private com.staynest.room.client.ReservationServiceClient reservationServiceClient;

    @Override
    public List<RoomResponse> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<RoomResponse> getAvailableRooms(String checkIn, String checkOut) {
        List<Room> available = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        if (checkIn == null || checkOut == null || reservationServiceClient == null) {
            return available.stream().map(this::mapToResponse).collect(Collectors.toList());
        }

        try {
            var resResponse = reservationServiceClient.getAllReservations(null);
            if (resResponse != null && resResponse.getData() instanceof List) {
                List<?> reservations = (List<?>) resResponse.getData();
                java.time.LocalDate searchIn = java.time.LocalDate.parse(checkIn);
                java.time.LocalDate searchOut = java.time.LocalDate.parse(checkOut);

                java.util.Map<Integer, Long> bookedCounts = reservations.stream()
                        .filter(obj -> obj instanceof java.util.Map)
                        .map(obj -> (java.util.Map<?, ?>) obj)
                        .filter(map -> {
                            Object status = map.get("status");
                            if (status == null) return false;
                            String st = status.toString();
                            if (!"CONFIRMED".equalsIgnoreCase(st) && !"CHECKEDIN".equalsIgnoreCase(st)) return false;
                            Object inObj = map.get("checkInDate");
                            Object outObj = map.get("checkOutDate");
                            if (inObj == null || outObj == null) return false;
                            java.time.LocalDate resIn = java.time.LocalDate.parse(inObj.toString());
                            java.time.LocalDate resOut = java.time.LocalDate.parse(outObj.toString());
                            return resIn.isBefore(searchOut) && resOut.isAfter(searchIn);
                        })
                        .filter(map -> map.get("roomTypeId") != null)
                        .collect(Collectors.groupingBy(map -> Integer.parseInt(map.get("roomTypeId").toString()), Collectors.counting()));

                java.util.Map<Integer, List<Room>> roomsByType = available.stream()
                        .collect(Collectors.groupingBy(r -> r.getRoomType().getRoomTypeId()));

                List<Room> filtered = new java.util.ArrayList<>();
                for (var entry : roomsByType.entrySet()) {
                    Integer typeId = entry.getKey();
                    List<Room> typeRooms = entry.getValue();
                    long booked = bookedCounts.getOrDefault(typeId, 0L);
                    int remainingCount = Math.max(0, (int) (typeRooms.size() - booked));
                    for (int i = 0; i < remainingCount && i < typeRooms.size(); i++) {
                        filtered.add(typeRooms.get(i));
                    }
                }
                return filtered.stream().map(this::mapToResponse).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to filter availability against reservation-service: {}", e.getMessage());
        }

        return available.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<RoomResponse> getRoomsByType(Integer roomTypeId) {
        return roomRepository.findByRoomType_RoomTypeId(roomTypeId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public RoomResponse updateRoomStatus(Integer id, RoomStatus status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setStatus(status);
        Room updated = roomRepository.save(room);
        log.info("Room {} status updated to {}", id, status);
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