package com.staynest.housekeeping.serviceimpl;

import com.staynest.housekeeping.dto.HousekeepingTaskRequest;
import com.staynest.housekeeping.dto.HousekeepingTaskResponse;
import com.staynest.housekeeping.entity.HousekeepingTask;
import com.staynest.housekeeping.enums.TaskStatus;
import com.staynest.housekeeping.exception.ResourceNotFoundException;
import com.staynest.housekeeping.repository.HousekeepingTaskRepository;
import com.staynest.housekeeping.service.HousekeepingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HousekeepingTaskServiceImpl implements HousekeepingTaskService {

    private final HousekeepingTaskRepository taskRepository;
    private final com.staynest.housekeeping.client.NotificationServiceClient notificationServiceClient;

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(java.util.Map.of(
                    "userId", userId, "category", "HOUSEKEEPING", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send HOUSEKEEPING notification to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public HousekeepingTaskResponse createTask(HousekeepingTaskRequest request) {
        HousekeepingTask task = HousekeepingTask.builder()
                .roomId(request.getRoomId())
                .taskType(request.getTaskType())
                .assignedToId(request.getAssignedToId())
                .assignedDate(request.getAssignedDate())
                .build();

        HousekeepingTask saved = taskRepository.save(task);
        log.info("Housekeeping task created: {}", saved.getTaskId());
        return mapToResponse(saved);
    }

    @Override
    public HousekeepingTaskResponse assignTask(Integer taskId, Integer staffId) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        task.setAssignedToId(staffId);
        task.setStatus(TaskStatus.INPROGRESS);
        HousekeepingTask updated = taskRepository.save(task);
        log.info("Task {} assigned to staff {}", taskId, staffId);
        notify(staffId, "You have been assigned housekeeping task #" + updated.getTaskId()
                + " for room #" + updated.getRoomId() + ".");
        return mapToResponse(updated);
    }

    @Override
    public HousekeepingTaskResponse updateTaskStatus(Integer taskId, TaskStatus status) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        task.setStatus(status);
        if (status == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }
        HousekeepingTask updated = taskRepository.save(task);
        log.info("Task {} status updated to {}", taskId, status);
        return mapToResponse(updated);
    }

    @Override
    public HousekeepingTaskResponse getTaskById(Integer taskId) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        return mapToResponse(task);
    }

    @Override
    public List<HousekeepingTaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<HousekeepingTaskResponse> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<HousekeepingTaskResponse> getTasksByRoomId(Integer roomId) {
        return taskRepository.findByRoomId(roomId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<HousekeepingTaskResponse> getTasksByAssignedTo(Integer staffId) {
        return taskRepository.findByAssignedToId(staffId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    private HousekeepingTaskResponse mapToResponse(HousekeepingTask task) {
        return HousekeepingTaskResponse.builder()
                .taskId(task.getTaskId())
                .roomId(task.getRoomId())
                .taskType(task.getTaskType())
                .assignedToId(task.getAssignedToId())
                .assignedDate(task.getAssignedDate())
                .completedAt(task.getCompletedAt())
                .status(task.getStatus())
                .build();
    }
}