package com.staynest.fb.serviceimpl;

import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.client.FeignErrors;
import com.staynest.fb.client.FrontDeskServiceClient;
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

    @Override
    @Transactional
    public DiningReservationResponse createReservation(DiningReservationRequest request) {
        // The reservation date can't be in the past.
        if (request.getDate() == null || request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Reservation date cannot be in the past");
        }
        // The guest must exist.
        validateGuest(request.getGuestId());
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

    private void validateGuest(Integer guestId) {
        if (guestId == null) {
            throw new BadRequestException("Guest ID is required");
        }
        try {
            var resp = reservationServiceClient.getGuestById(guestId);
            if (resp == null || resp.getData() == null) {
                throw new BadRequestException("Invalid Guest ID: " + guestId + " (no such guest)");
            }
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