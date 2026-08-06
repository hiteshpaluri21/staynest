package com.staynest.housekeeping.client;

import com.staynest.housekeeping.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Used to check whether a room is currently occupied, because some task types only make sense
 * while a guest is in the room (see {@code HousekeepingTaskServiceImpl.validateTaskAppliesToRoom}).
 */
@FeignClient(name = "ROOM-SERVICE")
public interface RoomServiceClient {

    @GetMapping("/api/rooms/{id}")
    ApiResponse<Map<String, Object>> getRoomById(@PathVariable Integer id);
}
