package com.staynest.reservation.serviceimpl;

import com.staynest.reservation.client.RoomServiceClient;
import com.staynest.reservation.dto.ReservationRequest;
import com.staynest.reservation.dto.ReservationResponse;
import com.staynest.reservation.entity.GuestProfile;
import com.staynest.reservation.entity.Reservation;
import com.staynest.reservation.enums.ReservationStatus;
import com.staynest.reservation.exception.BadRequestException;
import com.staynest.reservation.exception.ResourceNotFoundException;
import com.staynest.reservation.repository.GuestProfileRepository;
import com.staynest.reservation.repository.ReservationRepository;
import com.staynest.reservation.service.ReservationService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    @Autowired
    private RoomServiceClient roomServiceClient;

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // Validate guest exists or auto-create GuestProfile for new guest matching request.getGuestId()
        GuestProfile guest = guestProfileRepository.findById(request.getGuestId())
                .orElseGet(() -> {
                    String email = "guest" + request.getGuestId() + "@staynest.com";
                    return guestProfileRepository.findByEmail(email)
                            .orElseGet(() -> {
                                log.info("GuestProfile not found for guestId {}, creating default guest profile", request.getGuestId());
                                GuestProfile gp = new GuestProfile();
                                gp.setName("Guest #" + request.getGuestId());
                                gp.setEmail(email);
                                gp.setStatus(com.staynest.reservation.enums.GuestStatus.ACTIVE);
                                gp.setLoyaltyTier(com.staynest.reservation.enums.LoyaltyTier.NONE);
                                return guestProfileRepository.save(gp);
                            });
                });

        // Validate Check-In / Check-Out dates
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new BadRequestException("Check-In and Check-Out dates are required.");
        }
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-In date cannot be in the past.");
        }
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BadRequestException("Check-Out date must be after Check-In date.");
        }

        // Validate roomType
        try {
            roomServiceClient.getRoomTypeById(request.getRoomTypeId());
        } catch (FeignException.NotFound e) {
            throw new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId());
        } catch (Exception e) {
            log.error("room-service call failed while validating RoomTypeId {}", request.getRoomTypeId(), e);
            throw new BadRequestException("Unable to validate RoomTypeId " + request.getRoomTypeId()
                    + " (room-service error: " + e.getMessage() + ")");
        }

        // Check date-overlapping reservations against total physical rooms for roomTypeId
        try {
            var roomsResp = roomServiceClient.getAllRooms();
            if (roomsResp != null && roomsResp.getData() instanceof List) {
                List<?> list = (List<?>) roomsResp.getData();
                long totalPhysicalRooms = list.stream().filter(obj -> obj instanceof java.util.Map)
                        .map(obj -> (java.util.Map<?, ?>) obj)
                        .filter(map -> {
                            Object typeId = map.get("roomTypeId");
                            Object status = map.get("status");
                            return typeId != null && Integer.parseInt(typeId.toString()) == request.getRoomTypeId()
                                    && status != null && !"OUT_OF_SERVICE".equalsIgnoreCase(status.toString());
                        })
                        .count();

                List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                        request.getRoomTypeId(),
                        request.getCheckInDate(),
                        request.getCheckOutDate()
                );

                if (totalPhysicalRooms > 0 && overlapping.size() >= totalPhysicalRooms) {
                    throw new BadRequestException("No rooms available for the selected dates ("
                            + request.getCheckInDate() + " to " + request.getCheckOutDate() + "). All rooms of this type are already reserved.");
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Room availability check warning: {}", e.getMessage());
        }

        // Validate ratePlan if specified
        Integer finalRatePlanId = request.getRatePlanId();
        if (finalRatePlanId != null && finalRatePlanId > 0) {
            try {
                roomServiceClient.getRatePlanById(finalRatePlanId);
            } catch (Exception e) {
                log.warn("RatePlanId {} validation failed, defaulting ratePlanId: {}", finalRatePlanId, e.getMessage());
                finalRatePlanId = 1;
            }
        } else {
            finalRatePlanId = 1;
        }

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoomTypeId(request.getRoomTypeId());
        reservation.setRatePlanId(finalRatePlanId);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNights(request.getNights());
        reservation.setAdults(request.getAdults());
        reservation.setChildren(request.getChildren() != null ? request.getChildren() : 0);
        reservation.setTotalAmount(request.getTotalAmount());
        reservation.setBookingChannel(request.getBookingChannel() != null ? request.getBookingChannel() : com.staynest.reservation.enums.BookingChannel.DIRECT);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created: {}", saved.getReservationId());
        return mapToResponse(saved);
    }

    @Override
    public ReservationResponse getReservationById(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        return mapToResponse(reservation);
    }

    @Override
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getReservationsByGuest(Integer guestId) {
        List<Reservation> list = reservationRepository.findByGuest_GuestId(guestId);
        if (list.isEmpty()) {
            String email = "guest" + guestId + "@staynest.com";
            GuestProfile gp = guestProfileRepository.findByEmail(email).orElse(null);
            if (gp != null && !gp.getGuestId().equals(guestId)) {
                list = reservationRepository.findByGuest_GuestId(gp.getGuestId());
            }
        }
        return list.stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getUpcomingReservations(LocalDate date) {
        return reservationRepository.findByCheckInDateBetween(date, date.plusDays(30)).stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() == ReservationStatus.CHECKEDIN) {
            throw new BadRequestException("Cannot cancel a checked-in reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation {} cancelled", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationStatus(Integer id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        reservation.setStatus(status);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation {} status updated to {}", id, status);
        return mapToResponse(updated);
    }

    private ReservationResponse mapToResponse(Reservation r) {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(r.getReservationId());
        response.setGuestId(r.getGuest().getGuestId());
        response.setGuestName(r.getGuest().getName());
        response.setRoomTypeId(r.getRoomTypeId());
        response.setRatePlanId(r.getRatePlanId());
        response.setCheckInDate(r.getCheckInDate());
        response.setCheckOutDate(r.getCheckOutDate());
        response.setNights(r.getNights());
        response.setAdults(r.getAdults());
        response.setChildren(r.getChildren());
        response.setTotalAmount(r.getTotalAmount());
        response.setBookingChannel(r.getBookingChannel());
        response.setStatus(r.getStatus());
        return response;
    }
}