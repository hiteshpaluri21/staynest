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
import java.time.temporal.ChronoUnit;
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

    @Autowired
    private com.staynest.reservation.client.IamServiceClient iamServiceClient;

    @Autowired
    private com.staynest.reservation.client.NotificationServiceClient notificationServiceClient;

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String category, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(java.util.Map.of(
                    "userId", userId, "category", category, "message", message));
        } catch (Exception e) {
            log.warn("Failed to send {} notification to user {}: {}", category, userId, e.getMessage());
        }
    }

    /** Fan out a notification to every active staff member of a role. Best-effort. */
    private void notifyStaffByRole(String role, String category, String message) {
        try {
            var resp = iamServiceClient.getUsersByRole(role);
            var staff = resp != null ? resp.getData() : null;
            if (staff == null) return;
            for (var u : staff) {
                Object id = u.get("userId");
                if (id instanceof Number n) {
                    notify(n.intValue(), category, message);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve {} staff for notification: {}", role, e.getMessage());
        }
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // Validate guest exists or auto-create GuestProfile for new guest matching request.getGuestId()
        GuestProfile guest = guestProfileRepository.findById(request.getGuestId())
                .orElseGet(() -> {
                    String fallbackEmail = "guest" + request.getGuestId() + "@staynest.com";
                    // Prefer the acting user's real IAM name/email; fall back to a placeholder
                    // if the identity can't be resolved (e.g. staff booking on someone's behalf).
                    String[] identity = resolveGuestIdentity(request.getGuestId(), fallbackEmail);
                    String realName = identity[0];
                    String realEmail = identity[1];
                    return guestProfileRepository.findByEmail(realEmail)
                            .orElseGet(() -> {
                                log.info("GuestProfile not found for guestId {}, creating profile ({})", request.getGuestId(), realName);
                                GuestProfile gp = new GuestProfile();
                                gp.setName(realName);
                                gp.setEmail(realEmail);
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

        // Derive nights from the dates rather than trusting the client-supplied value.
        if (request.getCheckOutDate() == null || request.getCheckInDate() == null
                || !request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }
        int nights = (int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoomTypeId(request.getRoomTypeId());
        reservation.setRatePlanId(finalRatePlanId);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNights(nights);
        reservation.setAdults(request.getAdults());
        reservation.setChildren(request.getChildren() != null ? request.getChildren() : 0);
        reservation.setTotalAmount(request.getTotalAmount());
        reservation.setBookingChannel(request.getBookingChannel() != null ? request.getBookingChannel() : com.staynest.reservation.enums.BookingChannel.DIRECT);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created: {}", saved.getReservationId());
        notify(guest.getGuestId(), "RESERVATION",
                "Your reservation #" + saved.getReservationId() + " is confirmed for "
                        + saved.getCheckInDate() + " to " + saved.getCheckOutDate() + ".");
        // Alert front-desk staff of the new booking so it appears in their inbox.
        notifyStaffByRole("FRONTDESK", "FRONTDESK",
                "New reservation #" + saved.getReservationId() + " (" + guest.getName() + ") for "
                        + saved.getCheckInDate() + " to " + saved.getCheckOutDate() + ".");
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
        // Upcoming arrivals = every reservation still expected to arrive on/after the given date.
        // Previously this was capped at a 30-day window and excluded CHECKEDIN, so confirmed
        // reservations beyond 30 days (or already checked in) went missing from the arrivals list
        // even though they appeared under "Confirmed".
        return reservationRepository.findByCheckInDateGreaterThanEqual(date).stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                        || r.getStatus() == ReservationStatus.CHECKEDIN)
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

    /**
     * Resolves the real name/email for an auto-created guest profile from IAM, using the
     * currently authenticated user's email (the JWT subject). Only trusted when the resolved
     * IAM userId matches the requested guestId, so a staff member booking on someone's behalf
     * doesn't stamp their own name onto the guest. Returns {name, email}, falling back to a
     * "Guest #<id>" placeholder and synthetic email when identity can't be confirmed.
     */
    private String[] resolveGuestIdentity(Integer guestId, String fallbackEmail) {
        String name = "Guest #" + guestId;
        String email = fallbackEmail;
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                var resp = iamServiceClient.getUserByEmail(auth.getName());
                if (resp != null && resp.getData() != null) {
                    java.util.Map<String, Object> u = resp.getData();
                    Object uid = u.get("userId");
                    if (uid != null && Integer.parseInt(uid.toString()) == guestId) {
                        if (u.get("name") != null && !u.get("name").toString().isBlank()) {
                            name = u.get("name").toString();
                        }
                        if (u.get("email") != null && !u.get("email").toString().isBlank()) {
                            email = u.get("email").toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve real identity for guestId {}: {}", guestId, e.getMessage());
        }
        return new String[]{name, email};
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