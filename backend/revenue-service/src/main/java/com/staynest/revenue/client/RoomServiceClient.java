package com.staynest.revenue.client;

import com.staynest.revenue.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "room-service")
public interface RoomServiceClient {

    @GetMapping("/api/rooms")
    ApiResponse<List<Map<String, Object>>> getAllRooms();
}                                                                                                                                                                                                                                                                                                                            