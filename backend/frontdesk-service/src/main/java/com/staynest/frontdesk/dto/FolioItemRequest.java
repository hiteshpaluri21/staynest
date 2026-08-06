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
    /**
     * Who posted the charge. Optional: the FolioItem column is nullable, and null honestly
     * records "actor unknown". It was @NotNull, which pushed callers into inventing a user id
     * (fb-service substituted 1) and quietly mis-attributed the audit trail.
     */
    private Integer postedBy;
}
