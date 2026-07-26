package com.staynest.frontdesk.dto;

import com.staynest.frontdesk.enums.ChargeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FolioItemRequest {
    @NotNull
    private ChargeType chargeType;
    @NotBlank
    private String description;
    @NotNull @Positive
    private BigDecimal amount;
    @NotNull
    private Integer postedBy;
}
