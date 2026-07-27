package com.staynest.room.service;

import com.staynest.room.dto.RoomRequest;
import com.staynest.room.dto.RoomResponse;
import com.staynest.room.enums.RoomStatus;

import java.util.List;

public interface RoomService {

    RoomResponse addRoom(RoomRequest request);
    List<RoomResponse> getAllRooms();
    RoomResponse getRoomById(Integer id);
    List<RoomResponse> getRoomsByStatus(RoomStatus status);
    List<RoomResponse> getAvailableRooms(String checkIn, String checkOut);
    List<RoomResponse> getRoomsByType(Integer roomTypeId);
    RoomResponse updateRoomStatus(Integer id, RoomStatus status);
}