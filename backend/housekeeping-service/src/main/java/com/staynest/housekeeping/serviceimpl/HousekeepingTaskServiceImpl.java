package com.staynest.housekeeping.serviceimpl;

import com.staynest.housekeeping.audit.AuditRecorder;
import com.staynest.housekeeping.dto.HousekeepingTaskRequest;
import com.staynest.housekeeping.dto.HousekeepingTaskResponse;
import com.staynest.housekeeping.entity.HousekeepingTask;
import com.staynest.housekeeping.enums.TaskStatus;
import com.staynest.housekeeping.enums.TaskType;
import com.staynest.housekeeping.exception.BadRequestException;
import com.staynest.housekeeping.exception.ResourceNotFoundException;
import com.staynest.housekeeping.repository.HousekeepingTaskRepository;
import com.staynest.housekeeping.service.HousekeepingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.staynest.housekeeping.client.NotificationServiceClient;
import com.staynest.housekeeping.client.RoomServiceClient;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HousekeepingTaskServiceImpl implements HousekeepingTaskService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "HOUSEKEEPINGTASK";

    private final AuditRecorder auditRecorder;

    private final HousekeepingTaskRepository taskRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final RoomServiceClient roomServiceClient;

    /**
     * Task types that only make sense while a guest is actually in the room: you cannot clean up
     * after a departure, service a stayover, or turn down a bed in a room nobody is occupying.
     * DEEPCLEAN is deliberately absent — that is scheduled maintenance work on any room.
     */
    private static final Set<TaskType> REQUIRES_OCCUPIED_ROOM = EnumSet.of(
            TaskType.CHECKOUT, TaskType.STAYOVERSERVICE, TaskType.TURNDOWN);

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(Map.of(
                    "userId", userId, "category", "HOUSEKEEPING", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send HOUSEKEEPING notification to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public HousekeepingTaskResponse createTask(HousekeepingTaskRequest request) {
        validateTaskAppliesToRoom(request.getTaskType(), request.getRoomId());

        HousekeepingTask task = HousekeepingTask.builder()
                .roomId(request.getRoomId())
                .taskType(request.getTaskType())
                .assignedToId(request.getAssignedToId())
                .assignedDate(request.getAssignedDate())
                .build();

        HousekeepingTask saved = taskRepository.save(task);
        log.info("Housekeeping task created: {}", saved.getTaskId());
        auditRecorder.record("CREATE", ENTITY, saved.getTaskId());
        return mapToResponse(saved);
    }

    /**
     * Rejects occupancy-dependent task types for a room that has no guest in it. A room-service
     * outage is not treated as a failure: the check is skipped and logged rather than blocking
     * front desk from raising work.
     */
    private void validateTaskAppliesToRoom(TaskType taskType, Integer roomId) {
        if (!REQUIRES_OCCUPIED_ROOM.contains(taskType) || roomId == null) {
            return;
        }
        String status;
        try {
            var resp = roomServiceClient.getRoomById(roomId);
            Object raw = resp != null && resp.getData() != null ? resp.getData().get("status") : null;
            status = raw != null ? raw.toString() : null;
        } catch (Exception e) {
            log.warn("Could not read room {} status; skipping the occupancy check for {}: {}",
                    roomId, taskType, e.getMessage());
            return;
        }
        if (status != null && !"OCCUPIED".equalsIgnoreCase(status)) {
            throw new BadRequestException("A " + taskType + " task needs a guest in the room, but room "
                    + roomId + " is " + status + ". Use DEEPCLEAN for an unoccupied room.");
        }
    }

    @Override
    @Transactional
    public HousekeepingTaskResponse assignTask(Integer taskId, Integer staffId) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        // Assigning is not the same as starting: front desk assigns the work, and the assigned
        // housekeeping staff member is the one who moves it to INPROGRESS. Forcing INPROGRESS here
        // also used to resurrect already-DONE tasks.
        task.setAssignedToId(staffId);
        HousekeepingTask updated = taskRepository.save(task);
        log.info("Task {} assigned to staff {}", taskId, staffId);
        auditRecorder.record("ASSIGN", ENTITY, taskId);
        notify(staffId, "You have been assigned housekeeping task #" + updated.getTaskId()
                + " for room #" + updated.getRoomId() + ".");
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public HousekeepingTaskResponse updateTaskStatus(Integer taskId, TaskStatus status) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        task.setStatus(status);
        if (status == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }
        HousekeepingTask updated = taskRepository.save(task);
        log.info("Task {} status updated to {}", taskId, status);
        auditRecorder.record("UPDATE_STATUS", ENTITY, taskId);
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