package com.staynest.room.service;

import com.staynest.room.dto.RoomTypeRequest;
import com.staynest.room.dto.RoomTypeResponse;
import com.staynest.room.enums.RatePlanStatus;

import java.util.List;

public interface RoomTypeService {

    RoomTypeResponse createRoomType(RoomTypeRequest request);
    List<RoomTypeResponse> getAllRoomTypes();
    RoomTypeResponse getRoomTypeById(Integer id);
    RoomTypeResponse updateRoomType(Integer id, RoomTypeRequest request);
    RoomTypeResponse updateStatus(Integer id, RatePlanStatus status);
}