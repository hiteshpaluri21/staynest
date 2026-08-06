package com.staynest.fb.controller;

import com.staynest.fb.dto.ApiResponse;
import com.staynest.fb.dto.DiningReservationRequest;
import com.staynest.fb.dto.DiningReservationResponse;
import com.staynest.fb.enums.DiningResStatus;
import com.staynest.fb.service.DiningReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dining-reservations")
@RequiredArgsConstructor
public class DiningReservationController {

    private final DiningReservationService reservationService;

    // Guests book their own table; F&B staff only seat/complete what has been booked, mirroring
    // how housekeeping and front desk process requests they don't raise themselves.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GUEST')")
    public ResponseEntity<ApiResponse<DiningReservationResponse>> create(@Valid @RequestBody DiningReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservation created", reservationService.createReservation(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DiningReservationResponse>>> getAll(
            @RequestParam(required = false) Integer guestId,
            @RequestParam(required = false) LocalDate date) {
        if (guestId != null) {
            return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationsByGuestId(guestId)));
        }
        if (date != null) {
            return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationsByDate(date)));
        }
        return ResponseEntity.ok(ApiResponse.success(reservationService.getAllReservations()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiningReservationResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationById(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<DiningReservationResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam DiningResStatus status) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.updateReservationStatus(id, status)));
    }

    // A guest booked the table, so a guest can call it off — seating and completing stay staff-only,
    // which is why cancelling is its own endpoint rather than part of the status transition above.
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER', 'GUEST')")
    public ResponseEntity<ApiResponse<DiningReservationResponse>> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled", reservationService.cancelReservation(id)));
    }
}