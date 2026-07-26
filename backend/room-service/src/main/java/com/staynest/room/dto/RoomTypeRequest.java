package com.staynest.room.dto;

import com.staynest.room.enums.RoomTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomTypeRequest {
    @NotNull
    private RoomTypeName name;
    @NotBlank
    private String bedConfiguration;
    @NotNull @Positive
    private Integer maxOccupancy;
    @NotNull @Positive
    private BigDecimal baseRate;
    private String amenitiesList;
}
	