package com.staynest.frontdesk.client;

import com.staynest.frontdesk.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.Map;

@FeignClient(name = "RESERVATION-SERVICE")
public interface ReservationServiceClient {

    @GetMapping("/api/reservations/{id}")
    ApiResponse<?> getReservationById(@PathVariable Integer id);

    // A stay's guestId is a reservation-service guest profile id, NOT an IAM userId. The guest
    // payload carries the userId behind the profile, which is who notifications go to.
    @GetMapping("/api/guests/{id}")
    ApiResponse<Map<String, Object>> getGuestById(@PathVariable Integer id);

    @PatchMapping("/api/reservations/{id}/status")
    ApiResponse<?> updateReservationStatus(@PathVariable Integer id, @RequestParam String status);
}