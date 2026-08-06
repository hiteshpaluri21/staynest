package com.staynest.fb.service;

import com.staynest.fb.dto.DiningReservationRequest;
import com.staynest.fb.dto.DiningReservationResponse;
import com.staynest.fb.enums.DiningResStatus;

import java.time.LocalDate;
import java.util.List;

public interface DiningReservationService {
    DiningReservationResponse createReservation(DiningReservationRequest request);
    DiningReservationResponse updateReservationStatus(Integer id, DiningResStatus status);
    DiningReservationResponse cancelReservation(Integer id);
    DiningReservationResponse getReservationById(Integer id);
    List<DiningReservationResponse> getAllReservations();
    List<DiningReservationResponse> getReservationsByGuestId(Integer guestId);
    List<DiningReservationResponse> getReservationsByDate(LocalDate date);
}