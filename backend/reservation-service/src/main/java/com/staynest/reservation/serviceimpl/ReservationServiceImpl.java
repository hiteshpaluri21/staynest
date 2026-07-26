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
        // Validate guest exists
        GuestProfile guest = guestProfileRepository.findById(request.getGuestId())
                .orElseThrow(() -> new BadRequestException("Invalid GuestId: " + request.getGuestId()));

        // Validate roomType and ratePlan exist (cross-service via Feign)
        try {
            roomServiceClient.getRoomTypeById(request.getRoomTypeId());
        } catch (FeignException.NotFound e) {
            throw new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId());
        } catch (Exception e) {
            log.error("room-service call failed while validating RoomTypeId {}", request.getRoomTypeId(), e);
            throw new BadRequestException("Unable to validate RoomTypeId " + request.getRoomTypeId()
                    + " (room-service error: " + e.getMessage() + ")");
        }

        try {
            roomServiceClient.getRatePlanById(request.getRatePlanId());
        } catch (FeignException.NotFound e) {
            throw new BadRequestException("Invalid RatePlanId: " + request.getRatePlanId());
        } catch (Exception e) {
            log.error("room-service call failed while validating RatePlanId {}", request.getRatePlanId(), e);
            throw new BadRequestException("Unable to validate RatePlanId " + request.getRatePlanId()
                    + " (room-service error: " + e.getMessage() + ")");
        }

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoomTypeId(request.getRoomTypeId());
        reservation.setRatePlanId(request.getRatePlanId());
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNights(request.getNights());
        reservation.setAdults(request.getAdults());
        reservation.setChildren(request.getChildren());
        reservation.setTotalAmount(request.getTotalAmount());
        reservation.setBookingChannel(request.getBookingChannel());

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
        return reservationRepository.findByGuest_GuestId(guestId).stream()
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