package com.staynest.fb.client;

import com.staynest.fb.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "RESERVATION-SERVICE")
public interface ReservationServiceClient {

    // A dining reservation's guestId is a reservation-service guest profile id, NOT an IAM userId —
    // the two are separate key spaces, so validation has to happen against this endpoint.
    @GetMapping("/api/guests/{id}")
    ApiResponse<Map<String, Object>> getGuestById(@PathVariable Integer id);
}
