package com.staynest.room.controller;

import com.staynest.room.dto.ApiResponse;
import com.staynest.room.dto.RoomRequest;
import com.staynest.room.dto.RoomResponse;
import com.staynest.room.enums.RoomStatus;
import com.staynest.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> create(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created", roomService.addRoom(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAll(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Integer roomTypeId) {
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(roomService.getRoomsByStatus(status)));
        }
        if (roomTypeId != null) {
            return ResponseEntity.ok(ApiResponse.success(roomService.getRoomsByType(roomTypeId)));
        }
        return ResponseEntity.ok(ApiResponse.success(roomService.getAllRooms()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoomResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam RoomStatus status) {
        return ResponseEntity.ok(ApiResponse.success(roomService.updateRoomStatus(id, status)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailable(
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        // Phase 1: Return all AVAILABLE rooms (simplified)
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomsByStatus(RoomStatus.AVAILABLE)));
    }
}
