package com.staynest.housekeeping.dto;

import com.staynest.housekeeping.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HousekeepingTaskRequest {
    @NotNull
    private Integer roomId;
    @NotNull
    private TaskType taskType;
    private Integer assignedToId;
    private LocalDate assignedDate;
}