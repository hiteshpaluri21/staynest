package com.staynest.room.client;

import com.staynest.room.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "RESERVATION-SERVICE", fallbackFactory = ReservationServiceClientFallback.class)
public interface ReservationServiceClient {

	@GetMapping("/api/reservations")
	ApiResponse<List<Map<String, Object>>> getAllReservations(@RequestParam(required = false) String status);
}
