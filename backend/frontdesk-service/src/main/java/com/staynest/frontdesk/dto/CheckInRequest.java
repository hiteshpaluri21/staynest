package com.staynest.frontdesk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {
    @NotNull
    private Integer reservationId;
    @NotNull
    private Integer roomId;
}