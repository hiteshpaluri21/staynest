package com.staynest.revenue.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HospitalityReportResponse {
    private Integer reportId;
    private String scope;
    private String metrics;
    private LocalDateTime generatedDate;
}