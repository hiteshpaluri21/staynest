package com.staynest.room.dto;

import com.staynest.room.enums.RatePlanName;
import com.staynest.room.enums.RatePlanStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class RatePlanResponse {
    private Integer ratePlanId;
    private Integer roomTypeId;
    private String roomTypeName;
    private RatePlanName name;
    private BigDecimal pricePerNight;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean mealPlanIncluded;
    private RatePlanStatus status;
}