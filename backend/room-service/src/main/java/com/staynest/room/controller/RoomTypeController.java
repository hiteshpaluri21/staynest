package com.staynest.room.controller;

import com.staynest.room.dto.ApiResponse;
import com.staynest.room.dto.RoomTypeRequest;
import com.staynest.room.dto.RoomTypeResponse;
import com.staynest.room.enums.RatePlanStatus;
import com.staynest.room.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> create(@Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("RoomType created", roomTypeService.createRoomType(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(roomTypeService.getAllRoomTypes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(roomTypeService.getRoomTypeById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("RoomType updated", roomTypeService.updateRoomType(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam RatePlanStatus status) {
        return ResponseEntity.ok(ApiResponse.success(roomTypeService.updateStatus(id, status)));
    }
}