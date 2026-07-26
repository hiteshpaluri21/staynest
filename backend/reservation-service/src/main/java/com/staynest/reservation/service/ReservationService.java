package com.staynest.reservation.service;

import com.staynest.reservation.dto.ReservationRequest;
import com.staynest.reservation.dto.ReservationResponse;
import com.staynest.reservation.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {

    ReservationResponse createReservation(ReservationRequest request);
    ReservationResponse getReservationById(Integer id);
    List<ReservationResponse> getAllReservations();
    List<ReservationResponse> getReservationsByGuest(Integer guestId);
    List<ReservationResponse> getReservationsByStatus(ReservationStatus status);
    List<ReservationResponse> getUpcomingReservations(LocalDate date);
    ReservationResponse cancelReservation(Integer id);
    ReservationResponse updateReservationStatus(Integer id, ReservationStatus status);
}