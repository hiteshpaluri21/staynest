package com.staynest.fb.dto;

import com.staynest.fb.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FBOrderRequest {
    @NotNull
    private Integer stayId;
    @NotNull
    private OrderType orderType;
    @NotBlank
    private String itemsJson;
    @NotNull
    private Integer placedBy;
}