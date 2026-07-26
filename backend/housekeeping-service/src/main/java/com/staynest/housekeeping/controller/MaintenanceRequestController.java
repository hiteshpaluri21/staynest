package com.staynest.housekeeping.controller;

import com.staynest.housekeeping.dto.ApiResponse;
import com.staynest.housekeeping.dto.MaintenanceRequestDto;
import com.staynest.housekeeping.dto.MaintenanceResponse;
import com.staynest.housekeeping.enums.MaintenanceStatus;
import com.staynest.housekeeping.service.MaintenanceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-requests")
@RequiredArgsConstructor
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> create(@Valid @RequestBody MaintenanceRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Maintenance request created", maintenanceService.reportIssue(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceResponse>>> getAll(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) MaintenanceStatus status) {
        if (roomId != null) {
            return ResponseEntity.ok(ApiResponse.success(maintenanceService.getRequestsByRoomId(roomId)));
        }
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(maintenanceService.getRequestsByStatus(status)));
        }
        return ResponseEntity.ok(ApiResponse.success(maintenanceService.getAllRequests()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceService.getRequestById(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam MaintenanceStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", maintenanceService.updateStatus(id, status)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> resolve(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Request resolved", maintenanceService.resolveRequest(id)));
    }
}