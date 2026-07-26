package com.staynest.frontdesk.client;

import com.staynest.frontdesk.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;

@FeignClient(name = "reservation-service")
public interface ReservationServiceClient {

    @GetMapping("/api/reservations/{id}")
    ApiResponse<?> getReservationById(@PathVariable Integer id);

    @PatchMapping("/api/reservations/{id}/status")
    ApiResponse<?> updateReservationStatus(@PathVariable Integer id, @RequestParam String status);
}