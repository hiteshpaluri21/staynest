package com.staynest.housekeeping.dto;

import com.staynest.housekeeping.enums.MaintenancePriority;
import com.staynest.housekeeping.enums.MaintenanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MaintenanceResponse {
    private Integer requestId;
    private Integer roomId;
    private Integer reportedBy;
    private String issueDescription;
    private MaintenancePriority priority;
    private LocalDate raisedDate;
    private LocalDate resolvedDate;
    private MaintenanceStatus status;
}