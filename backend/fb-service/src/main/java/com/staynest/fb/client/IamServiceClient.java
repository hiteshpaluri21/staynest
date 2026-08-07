package com.staynest.fb.client;

import com.staynest.fb.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * Resolves staff to notify. A distinct contextId from AuditServiceClient, which also points at
 * iam-service — Feign requires one per interface sharing a service name.
 */
@FeignClient(name = "IAM-SERVICE", contextId = "fbIamServiceClient")
public interface IamServiceClient {

    // Active staff of a role, so new dining bookings can be announced to F&B managers.
    @GetMapping("/api/users/role/{role}")
    ApiResponse<List<Map<String, Object>>> getUsersByRole(@PathVariable String role);
}
