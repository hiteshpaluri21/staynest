package com.staynest.room.dto;

import com.staynest.room.enums.RoomTypeName;
import com.staynest.room.enums.RatePlanStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class RoomTypeResponse {
    private Integer roomTypeId;
    private RoomTypeName name;
    private String bedConfiguration;
    private Integer maxOccupancy;
    private BigDecimal baseRate;
    private String amenitiesList;
    private RatePlanStatus status;
}