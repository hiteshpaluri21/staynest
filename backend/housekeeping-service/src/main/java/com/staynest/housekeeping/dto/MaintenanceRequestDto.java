package com.staynest.housekeeping.dto;

import com.staynest.housekeeping.enums.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaintenanceRequestDto {
    @NotNull
    private Integer roomId;
    @NotNull
    private Integer reportedBy;
    @NotBlank
    private String issueDescription;
    @NotNull
    private MaintenancePriority priority;
}