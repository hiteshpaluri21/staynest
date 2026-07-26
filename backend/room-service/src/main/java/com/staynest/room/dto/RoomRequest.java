package com.staynest.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RoomRequest {
    @NotBlank
    private String roomNumber;
    @NotNull @Positive
    private Integer floor;
    @NotNull
    private Integer roomTypeId;
}