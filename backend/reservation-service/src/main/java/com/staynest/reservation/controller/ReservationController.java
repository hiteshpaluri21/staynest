package com.staynest.reservation.controller;

import com.staynest.reservation.dto.ApiResponse;
import com.staynest.reservation.dto.ReservationRequest;
import com.staynest.reservation.dto.ReservationResponse;
import com.staynest.reservation.enums.ReservationStatus;
import com.staynest.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservation created", reservationService.createReservation(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getAll(
            @RequestParam(required = false) Integer guestId,
            @RequestParam(required = false) ReservationStatus status) {
        if (guestId != null) {
            return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationsByGuest(guestId)));
        }
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationsByStatus(status)));
        }
        return ResponseEntity.ok(ApiResponse.success(reservationService.getAllReservations()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getUpcoming(
            @RequestParam(required = false) LocalDate date) {
        LocalDate searchDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(reservationService.getUpcomingReservations(searchDate)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationById(id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled", reservationService.cancelReservation(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam ReservationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.updateReservationStatus(id, status)));
    }
}