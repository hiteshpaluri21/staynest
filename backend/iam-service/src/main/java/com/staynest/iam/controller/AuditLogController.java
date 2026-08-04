package com.staynest.iam.controller;

import com.staynest.iam.dto.ApiResponse;
import com.staynest.iam.entity.AuditLog;
import com.staynest.iam.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getAll(pageable)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getByUserId(userId)));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByAction(@PathVariable String action) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getByAction(action)));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByTimestampRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getByTimestampRange(start, end)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> logAction(
            @RequestParam Integer userId,
            @RequestParam String action,
            @RequestParam String entityType,
            @RequestParam(required = false) Integer entityId) {
        auditLogService.logAction(userId, action, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Action logged", null));
    }
}