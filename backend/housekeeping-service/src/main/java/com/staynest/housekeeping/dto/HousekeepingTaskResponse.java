package com.staynest.housekeeping.dto;

import com.staynest.housekeeping.enums.TaskStatus;
import com.staynest.housekeeping.enums.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class HousekeepingTaskResponse {
    private Integer taskId;
    private Integer roomId;
    private TaskType taskType;
    private Integer assignedToId;
    private LocalDate assignedDate;
    private LocalDateTime completedAt;
    private TaskStatus status;
}