package com.staynest.revenue.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class KpiSummaryResponse {
    private Double occupancyRate;
    private BigDecimal adr;
    private BigDecimal revPAR;
    private Double avgLengthOfStay;
    private BigDecimal fbRevenue;
    private Double guestSatisfactionScore;
}	