package com.staynest.frontdesk.client;

import com.staynest.frontdesk.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Used to raise the CHECKOUT cleaning task automatically when a guest checks out, so the room
 * always enters the housekeeping board without anyone having to remember to add it.
 */
@FeignClient(name = "HOUSEKEEPING-SERVICE")
public interface HousekeepingServiceClient {

    @PostMapping("/api/housekeeping-tasks")
    ApiResponse<?> createTask(@RequestBody Map<String, Object> request);
}
