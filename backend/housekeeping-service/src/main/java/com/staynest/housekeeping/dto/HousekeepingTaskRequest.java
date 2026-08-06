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
    // Every task must have an owner — an unassigned task is work nobody has agreed to do, and it
    // sat on the board indefinitely. Front desk picks the staff member when raising the task.
    @NotNull(message = "must not be null (every task needs an assignee)")
    private Integer assignedToId;
    private LocalDate assignedDate;
}