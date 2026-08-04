package com.staynest.reservation.client;

import com.staynest.reservation.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "IAM-SERVICE", fallbackFactory = IamServiceClientFallback.class)
public interface IamServiceClient {

    // Open endpoint (no role restriction) — used to resolve the acting user's real
    // name/email from their JWT subject (email) when auto-creating a guest profile.
    @GetMapping("/api/users/email/{email}")
    ApiResponse<Map<String, Object>> getUserByEmail(@PathVariable String email);

    // Resolve active staff of a role (e.g. FRONTDESK) so they can be notified of new reservations.
    @GetMapping("/api/users/role/{role}")
    ApiResponse<List<Map<String, Object>>> getUsersByRole(@PathVariable String role);
}
