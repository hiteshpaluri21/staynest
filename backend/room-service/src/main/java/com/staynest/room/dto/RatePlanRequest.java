package com.staynest.room.dto;

import com.staynest.room.enums.RatePlanName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RatePlanRequest {
    @NotNull
    private Integer roomTypeId;
    @NotNull
    private RatePlanName name;
    @NotNull @Positive
    private BigDecimal pricePerNight;
    @NotNull
    private LocalDate validFrom;
    @NotNull
    private LocalDate validTo;
    private Boolean mealPlanIncluded;
}