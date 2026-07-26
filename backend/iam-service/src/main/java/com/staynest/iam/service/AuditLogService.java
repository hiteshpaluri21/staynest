package com.staynest.iam.service;

import com.staynest.iam.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

    void logAction(Integer userId, String action, String entityType, Integer entityId);
    List<AuditLog> getByUserId(Integer userId);
    List<AuditLog> getByAction(String action);
    List<AuditLog> getByTimestampRange(LocalDateTime start, LocalDateTime end);
    Page<AuditLog> getAll(Pageable pageable);
}