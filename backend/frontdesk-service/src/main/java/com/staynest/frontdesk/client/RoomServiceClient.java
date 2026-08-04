package com.staynest.frontdesk.client;

import com.staynest.frontdesk.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ROOM-SERVICE", fallbackFactory = RoomServiceClientFallback.class)
public interface RoomServiceClient {

    @PatchMapping("/api/rooms/{id}/status")
    ApiResponse<?> updateRoomStatus(@PathVariable Integer id, @RequestParam String status);
}