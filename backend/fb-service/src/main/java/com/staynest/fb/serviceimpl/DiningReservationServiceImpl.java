package com.staynest.fb.serviceimpl;

import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.client.FeignErrors;
import com.staynest.fb.client.FrontDeskServiceClient;
import com.staynest.fb.client.IamServiceClient;
import com.staynest.fb.client.NotificationServiceClient;
import com.staynest.fb.client.ReservationServiceClient;
import com.staynest.fb.dto.DiningReservationRequest;
import com.staynest.fb.dto.DiningReservationResponse;
import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.DiningReservationRepository;
import com.staynest.fb.service.DiningReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiningReservationServiceImpl implements DiningReservationService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "DININGRESERVATION";

    private final AuditRecorder auditRecorder;

    private final DiningReservationRepository reservationRepository;
    private final ReservationServiceClient reservationServiceClient;
    private final FrontDeskServiceClient frontDeskServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final IamServiceClient iamServiceClient;

    @Override
    @Transactional
    public DiningReservationResponse createReservation(DiningReservationRequest request) {
        // The reservation date can't be in the past.
        if (request.getDate() == null || request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Reservation date cannot be in the past");
        }
        // The guest must exist. Their profile also carries the account to notify.
        Map<String, Object> guest = validateGuest(request.getGuestId());
        // The stay is optional, but if supplied it must be a real stay.
        if (request.getStayId() != null) {
            validateStay(request.getStayId());
        }
        LocalTime endTime = resolveEndTime(request.getTime(), request.getEndTime());
        rejectClashingBooking(request, endTime);

        DiningReservation reservation = DiningReservation.builder()
                .guestId(request.getGuestId())
                .stayId(request.getStayId())
                .restaurantOutlet(request.getRestaurantOutlet())
                .date(request.getDate())
                .time(request.getTime())
                .endTime(endTime)
                .covers(request.getCovers())
                .build();

        DiningReservation saved = reservationRepository.save(reservation);
        log.info("Dining reservation created: {}", saved.getDiningResId());
        auditRecorder.record("CREATE", ENTITY, saved.getDiningResId());

        // Booking a table sent nothing to anyone — the guest got no confirmation and F&B had to
        // notice the new row themselves. Both now mirror how a room booking is announced.
        String window = saved.getTime() + "–" + saved.getEndTime();
        notify(accountFor(guest), "Your table for " + saved.getCovers() + " at "
                + saved.getRestaurantOutlet() + " is confirmed for " + saved.getDate()
                + ", " + window + ".");
        notifyFbStaff("New table booked at " + saved.getRestaurantOutlet() + " on "
                + saved.getDate() + ", " + window + " for " + saved.getCovers()
                + " (booking #" + saved.getDiningResId() + ").");
        return mapToResponse(saved);
    }

    /** How long a table is held when the caller does not say. */
    private static final Duration DEFAULT_SITTING = Duration.ofMinutes(90);

    /** Bookings that still hold the outlet. A cancelled or completed sitting frees it. */
    private static final List<DiningResStatus> LIVE_STATUSES =
            List.of(DiningResStatus.CONFIRMED, DiningResStatus.SEATED);

    /**
     * The end of the sitting, defaulting to {@link #DEFAULT_SITTING} after the start.
     *
     * Rejects an end at or before the start, and one that runs past midnight — the booking is
     * filed against a single date, so a sitting that crosses into the next day cannot be
     * represented, let alone overlap-checked.
     */
    private LocalTime resolveEndTime(LocalTime start, LocalTime requestedEnd) {
        if (requestedEnd == null) {
            LocalTime end = start.plus(DEFAULT_SITTING);
            // plus() wraps around midnight; a late booking is held until the end of the day.
            return end.isAfter(start) ? end : LocalTime.MAX;
        }
        if (!requestedEnd.isAfter(start)) {
            throw new BadRequestException("The end time (" + requestedEnd
                    + ") must be after the start time (" + start + ")");
        }
        return requestedEnd;
    }

    /**
     * Holds the outlet for the whole sitting: while one party has it, nobody else may book an
     * overlapping window there, and the same guest cannot double-book either.
     *
     * The outlet is treated as a single exclusive space rather than a set of tables — there is
     * no table inventory in the model to allocate against.
     */
    private void rejectClashingBooking(DiningReservationRequest request, LocalTime endTime) {
        reservationRepository
                .findByRestaurantOutletIgnoreCaseAndDateAndStatusIn(
                        request.getRestaurantOutlet(), request.getDate(), LIVE_STATUSES)
                .stream()
                .filter(existing -> overlaps(existing, request.getTime(), endTime))
                .findFirst()
                .ifPresent(clash -> {
                    String window = clash.getTime() + "–" + endOf(clash);
                    if (clash.getGuestId().equals(request.getGuestId())) {
                        throw new BadRequestException("You already have a table at "
                                + request.getRestaurantOutlet() + " on " + request.getDate()
                                + " from " + window + ". Cancel that booking first if you want to change it.");
                    }
                    throw new BadRequestException(request.getRestaurantOutlet() + " is already booked on "
                            + request.getDate() + " from " + window
                            + ". Please choose a time outside that window.");
                });
    }

    /** Half-open overlap: a sitting that ends exactly as another begins does not clash. */
    private boolean overlaps(DiningReservation existing, LocalTime start, LocalTime end) {
        return existing.getTime().isBefore(end) && endOf(existing).isAfter(start);
    }

    /** An existing booking's end, falling back to the default sitting for rows written without one. */
    private LocalTime endOf(DiningReservation reservation) {
        if (reservation.getEndTime() != null) {
            return reservation.getEndTime();
        }
        LocalTime end = reservation.getTime().plus(DEFAULT_SITTING);
        return end.isAfter(reservation.getTime()) ? end : LocalTime.MAX;
    }

    @Override
    @Transactional
    public DiningReservationResponse updateReservationStatus(Integer id, DiningResStatus status) {
        DiningReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining reservation not found: " + id));
        reservation.setStatus(status);
        DiningReservation updated = reservationRepository.save(reservation);
        log.info("Dining reservation {} status updated to {}", id, status);
        auditRecorder.record("UPDATE_STATUS", ENTITY, id);
        // Only the states the guest would want to hear about; COMPLETED needs no announcement,
        // as they were at the table for it.
        if (status == DiningResStatus.SEATED) {
            notify(accountFor(updated.getGuestId()), "Your table at "
                    + updated.getRestaurantOutlet() + " is ready — please come through.");
        } else if (status == DiningResStatus.NOSHOW) {
            notify(accountFor(updated.getGuestId()), "Your table at " + updated.getRestaurantOutlet()
                    + " on " + updated.getDate() + " was released as a no-show.");
        }
        return mapToResponse(updated);
    }

    /**
     * Guests cancel their own bookings through this, so it deliberately does not go through
     * {@link #updateReservationStatus} (staff-only). Only a booking that has not been seated yet
     * can be cancelled — once the party is at the table, F&B closes it out as COMPLETED instead.
     */
    @Override
    @Transactional
    public DiningReservationResponse cancelReservation(Integer id) {
        DiningReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining reservation not found: " + id));

        if (reservation.getStatus() == DiningResStatus.CANCELLED) {
            throw new BadRequestException("Dining reservation " + id + " is already cancelled");
        }
        if (reservation.getStatus() != DiningResStatus.CONFIRMED) {
            throw new BadRequestException("Only a CONFIRMED reservation can be cancelled. "
                    + "Reservation " + id + " is " + reservation.getStatus());
        }

        reservation.setStatus(DiningResStatus.CANCELLED);
        DiningReservation updated = reservationRepository.save(reservation);
        log.info("Dining reservation {} cancelled", id);
        auditRecorder.record("CANCEL", ENTITY, id);
        // The slot is free again, which F&B need to know since it changes what they can seat.
        notify(accountFor(updated.getGuestId()), "Your table at " + updated.getRestaurantOutlet()
                + " on " + updated.getDate() + " has been cancelled.");
        notifyFbStaff("Booking #" + id + " at " + updated.getRestaurantOutlet() + " on "
                + updated.getDate() + " was cancelled — the slot is free again.");
        return mapToResponse(updated);
    }

    @Override
    public DiningReservationResponse getReservationById(Integer id) {
        DiningReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining reservation not found: " + id));
        return mapToResponse(reservation);
    }

    @Override
    public List<DiningReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<DiningReservationResponse> getReservationsByGuestId(Integer guestId) {
        return reservationRepository.findByGuestId(guestId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<DiningReservationResponse> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByDate(date).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Confirms the guest exists and returns their profile, which carries the IAM userId that
     * notifications are addressed to. A dining reservation's guestId is a reservation-service
     * profile id, so it must never be used as a userId directly.
     */
    private Map<String, Object> validateGuest(Integer guestId) {
        if (guestId == null) {
            throw new BadRequestException("Guest ID is required");
        }
        try {
            var resp = reservationServiceClient.getGuestById(guestId);
            if (resp == null || resp.getData() == null) {
                throw new BadRequestException("Invalid Guest ID: " + guestId + " (no such guest)");
            }
            return resp.getData();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FeignErrors.isNotFound(e)) {
                throw new BadRequestException("Invalid Guest ID: " + guestId + " (no such guest)");
            }
            log.error("reservation-service call failed while validating Guest ID {}", guestId, e);
            throw new BadRequestException("Unable to validate Guest ID " + guestId
                    + " (reservation-service error: " + e.getMessage() + ")");
        }
    }

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(Map.of(
                    "userId", userId, "category", "FB", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send FB notification to user {}: {}", userId, e.getMessage());
        }
    }

    /** Fan out to every active F&B manager, so a new booking lands in their queue. Best-effort. */
    private void notifyFbStaff(String message) {
        try {
            var resp = iamServiceClient.getUsersByRole("FBMANAGER");
            var staff = resp != null ? resp.getData() : null;
            if (staff == null) return;
            for (var u : staff) {
                Object id = u.get("userId");
                if (id instanceof Number n) {
                    notify(n.intValue(), message);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve FBMANAGER staff for notification: {}", e.getMessage());
        }
    }

    /**
     * The IAM account behind a guest profile, or null when there is none — a walk-in with no
     * login. Read off the profile rather than guessed from the guestId.
     */
    private static Integer accountFor(Map<String, Object> guest) {
        Object id = guest != null ? guest.get("userId") : null;
        return id instanceof Number n ? n.intValue() : null;
    }

    /** Re-reads the guest to address a booking made earlier. */
    private Integer accountFor(Integer guestId) {
        try {
            var resp = reservationServiceClient.getGuestById(guestId);
            return accountFor(resp != null ? resp.getData() : null);
        } catch (Exception e) {
            log.warn("Could not resolve the account for guest {}: {}", guestId, e.getMessage());
            return null;
        }
    }

    private void validateStay(Integer stayId) {
        try {
            var resp = frontDeskServiceClient.getStayById(stayId);
            if (resp == null || resp.getData() == null) {
                throw new BadRequestException("Invalid Stay ID: " + stayId + " (no such stay)");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FeignErrors.isNotFound(e)) {
                throw new BadRequestException("Invalid Stay ID: " + stayId + " (no such stay)");
            }
            log.error("frontdesk-service call failed while validating Stay ID {}", stayId, e);
            throw new BadRequestException("Unable to validate Stay ID " + stayId
                    + " (frontdesk-service error: " + e.getMessage() + ")");
        }
    }

    private DiningReservationResponse mapToResponse(DiningReservation dr) {
        return DiningReservationResponse.builder()
                .diningResId(dr.getDiningResId())
                .guestId(dr.getGuestId())
                .stayId(dr.getStayId())
                .restaurantOutlet(dr.getRestaurantOutlet())
                .date(dr.getDate())
                .time(dr.getTime())
                .endTime(dr.getEndTime())
                .covers(dr.getCovers())
                .status(dr.getStatus())
                .build();
    }
}