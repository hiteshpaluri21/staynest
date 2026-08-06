package com.staynest.reservation.client;

import com.staynest.reservation.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;

//@FeignClient(name = "room-service")
@FeignClient(name = "ROOM-SERVICE")
public interface RoomServiceClient {

    @GetMapping("/api/room-types/{id}")
    ApiResponse<?> getRoomTypeById(@PathVariable Integer id);

    @GetMapping("/api/rate-plans/{id}")
    ApiResponse<?> getRatePlanById(@PathVariable Integer id);

    @GetMapping("/api/rooms")
    ApiResponse<List<Map<String, Object>>> getAllRooms();
}