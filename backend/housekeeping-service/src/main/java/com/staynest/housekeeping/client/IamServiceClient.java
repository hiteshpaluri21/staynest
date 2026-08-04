package com.staynest.housekeeping.client;

import com.staynest.housekeeping.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * Resolves staff recipients (e.g. all HOUSEKEEPING users) so notifications can be
 * addressed to staff, not just the guest/reporter. Returns ACTIVE users of the role.
 */
@FeignClient(name = "IAM-SERVICE", fallbackFactory = IamServiceClientFallback.class)
public interface IamServiceClient {

    @GetMapping("/api/users/role/{role}")
    ApiResponse<List<Map<String, Object>>> getUsersByRole(@PathVariable String role);
}
