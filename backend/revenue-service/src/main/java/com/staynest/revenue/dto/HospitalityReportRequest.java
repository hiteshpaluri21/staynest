package com.staynest.revenue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HospitalityReportRequest {
    @NotBlank
    private String scope;
    private String metrics;
}