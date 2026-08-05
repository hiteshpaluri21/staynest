package com.staynest.housekeeping.controller;

import com.staynest.housekeeping.dto.ApiResponse;
import com.staynest.housekeeping.dto.HousekeepingTaskRequest;
import com.staynest.housekeeping.dto.HousekeepingTaskResponse;
import com.staynest.housekeeping.enums.TaskStatus;
import com.staynest.housekeeping.service.HousekeepingTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/housekeeping-tasks")
@RequiredArgsConstructor
public class HousekeepingTaskController {

    private final HousekeepingTaskService taskService;

    // Front desk raises housekeeping work; housekeeping staff carry it out.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> create(@Valid @RequestBody HousekeepingTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created", taskService.createTask(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HousekeepingTaskResponse>>> getAll(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Integer roomId) {
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByStatus(status)));
        }
        if (roomId != null) {
            return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByRoomId(roomId)));
        }
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllTasks()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskById(id)));
    }

    // Front desk assigns work to a specific housekeeping staff member.
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> assign(
            @PathVariable Integer id,
            @RequestParam Integer staffId) {
        return ResponseEntity.ok(ApiResponse.success("Task assigned", taskService.assignTask(id, staffId)));
    }

    // FRONTDESK included so it can cancel (SKIP) work it raised; HOUSEKEEPING progresses tasks.
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", taskService.updateTaskStatus(id, status)));
    }
}