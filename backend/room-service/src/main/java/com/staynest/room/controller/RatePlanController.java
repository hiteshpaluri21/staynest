package com.staynest.room.controller;

import com.staynest.room.dto.ApiResponse;
import com.staynest.room.dto.RatePlanRequest;
import com.staynest.room.dto.RatePlanResponse;
import com.staynest.room.enums.RatePlanStatus;
import com.staynest.room.service.RatePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rate-plans")
@RequiredArgsConstructor
public class RatePlanController {

    private final RatePlanService ratePlanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUEMANAGER')")
    public ResponseEntity<ApiResponse<RatePlanResponse>> create(@Valid @RequestBody RatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("RatePlan created", ratePlanService.createRatePlan(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RatePlanResponse>>> getAll(
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) LocalDate date) {
        if (roomTypeId != null && date != null) {
            return ResponseEntity.ok(ApiResponse.success(ratePlanService.getActivePlansForRoomType(roomTypeId, date)));
        }
        return ResponseEntity.ok(ApiResponse.success(ratePlanService.getAllRatePlans()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RatePlanResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(ratePlanService.getRatePlanById(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUEMANAGER')")
    public ResponseEntity<ApiResponse<RatePlanResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam RatePlanStatus status) {
        return ResponseEntity.ok(ApiResponse.success(ratePlanService.updateStatus(id, status)));
    }
}