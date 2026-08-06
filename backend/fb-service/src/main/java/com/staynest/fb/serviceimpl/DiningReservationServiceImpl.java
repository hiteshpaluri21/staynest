package com.staynest.fb.serviceimpl;

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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiningReservationServiceImpl implements DiningReservationService {

    private final DiningReservationRepository reservationRepository;
    private final ReservationServiceClient reservationServiceClient;
    private final FrontDeskServiceClient frontDeskServiceClient;

    @Override
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
        // One table per guest, per outlet, per slot. Only CONFIRMED and SEATED bookings
        // block a rebooking, so cancelling and booking the same slot again works.
        boolean alreadyBooked = reservationRepository
                .existsByGuestIdAndRestaurantOutletIgnoreCaseAndDateAndTimeAndStatusIn(
                        request.getGuestId(), request.getRestaurantOutlet(),
                        request.getDate(), request.getTime(),
                        List.of(DiningResStatus.CONFIRMED, DiningResStatus.SEATED));
        if (alreadyBooked) {
            throw new BadRequestException("You already have a table at " + request.getRestaurantOutlet()
                    + " on " + request.getDate() + " at " + request.getTime()
                    + ". Cancel that booking first if you want to change it.");
        }

        DiningReservation reservation = DiningReservation.builder()
                .guestId(request.getGuestId())
                .stayId(request.getStayId())
                .restaurantOutlet(request.getRestaurantOutlet())
                .date(request.getDate())
                .time(request.getTime())
                .covers(request.getCovers())
                .build();

        DiningReservation saved = reservationRepository.save(reservation);
        log.info("Dining reservation created: {}", saved.getDiningResId());
        return mapToResponse(saved);
    }

    @Override
    public DiningReservationResponse updateReservationStatus(Integer id, DiningResStatus status) {
        DiningReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining reservation not found: " + id));
        reservation.setStatus(status);
        DiningReservation updated = reservationRepository.save(reservation);
        log.info("Dining reservation {} status updated to {}", id, status);
        return mapToResponse(updated);
    }

    /**
     * Guests cancel their own bookings through this, so it deliberately does not go through
     * {@link #updateReservationStatus} (staff-only). Only a booking that has not been seated yet
     * can be cancelled — once the party is at the table, F&B closes it out as COMPLETED instead.
     */
    @Override
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
                .covers(dr.getCovers())
                .status(dr.getStatus())
                .build();
    }
}