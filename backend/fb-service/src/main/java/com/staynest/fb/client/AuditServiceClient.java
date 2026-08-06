package com.staynest.fb.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Writes to the shared audit_logs table, which lives in iam-service.
 *
 * Every service records through this one endpoint so the trail is in a single table rather
 * than scattered per-database.
 */
@FeignClient(name = "IAM-SERVICE", contextId = "auditServiceClient")
public interface AuditServiceClient {

    @PostMapping("/api/audit-logs")
    void logAction(@RequestParam Integer userId,
                   @RequestParam String action,
                   @RequestParam String entityType,
                   @RequestParam(required = false) Integer entityId);
}
