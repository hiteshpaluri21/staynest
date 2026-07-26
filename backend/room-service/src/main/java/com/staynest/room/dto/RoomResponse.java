package com.staynest.room.dto;

import com.staynest.room.enums.RoomStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoomResponse {
    private Integer roomId;
    private String roomNumber;
    private Integer floor;
    private Integer roomTypeId;
    private String roomTypeName;
    private RoomStatus status;
}