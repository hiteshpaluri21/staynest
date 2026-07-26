package com.staynest.housekeeping.service;

import com.staynest.housekeeping.dto.HousekeepingTaskRequest;
import com.staynest.housekeeping.dto.HousekeepingTaskResponse;
import com.staynest.housekeeping.enums.TaskStatus;

import java.util.List;

public interface HousekeepingTaskService {
    HousekeepingTaskResponse createTask(HousekeepingTaskRequest request);
    HousekeepingTaskResponse assignTask(Integer taskId, Integer staffId);
    HousekeepingTaskResponse updateTaskStatus(Integer taskId, TaskStatus status);
    HousekeepingTaskResponse getTaskById(Integer taskId);
    List<HousekeepingTaskResponse> getAllTasks();
    List<HousekeepingTaskResponse> getTasksByStatus(TaskStatus status);
    List<HousekeepingTaskResponse> getTasksByRoomId(Integer roomId);
    List<HousekeepingTaskResponse> getTasksByAssignedTo(Integer staffId);
}
