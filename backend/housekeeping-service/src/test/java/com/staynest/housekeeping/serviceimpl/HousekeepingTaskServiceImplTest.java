package com.staynest.housekeeping.serviceimpl;

import com.staynest.housekeeping.audit.AuditRecorder;
import com.staynest.housekeeping.client.NotificationServiceClient;
import com.staynest.housekeeping.client.RoomServiceClient;
import com.staynest.housekeeping.dto.HousekeepingTaskRequest;
import com.staynest.housekeeping.dto.HousekeepingTaskResponse;
import com.staynest.housekeeping.entity.HousekeepingTask;
import com.staynest.housekeeping.enums.TaskStatus;
import com.staynest.housekeeping.enums.TaskType;
import com.staynest.housekeeping.repository.HousekeepingTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cleaning work starts unstarted and gets a completion stamp only when it is actually done —
 * the two facts a housekeeping shift board reads.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HousekeepingTaskServiceImplTest {

    private static final int TASK_ID = 3;
    private static final int ROOM_ID = 101;

    @Mock private AuditRecorder auditRecorder;
    @Mock private HousekeepingTaskRepository taskRepository;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private RoomServiceClient roomServiceClient;
    @InjectMocks private HousekeepingTaskServiceImpl service;

    private static HousekeepingTaskRequest request(TaskType type) {
        HousekeepingTaskRequest req = new HousekeepingTaskRequest();
        req.setRoomId(ROOM_ID);
        req.setTaskType(type);
        req.setAssignedToId(12);
        req.setAssignedDate(LocalDate.parse("2026-08-10"));
        return req;
    }

    private static HousekeepingTask task(TaskStatus status) {
        return HousekeepingTask.builder()
                .taskId(TASK_ID)
                .roomId(ROOM_ID)
                .taskType(TaskType.DEEPCLEAN)
                .assignedToId(12)
                .status(status)
                .build();
    }

    /**
     * DEEPCLEAN applies to any room, so no occupancy lookup is involved — a new task lands as
     * PENDING with nothing completed yet.
     */
    @Test
    void createTask_savedPending() {
        when(taskRepository.save(any(HousekeepingTask.class))).thenAnswer(inv -> {
            HousekeepingTask t = inv.getArgument(0);
            t.setTaskId(TASK_ID);
            return t;
        });

        HousekeepingTaskResponse created = service.createTask(request(TaskType.DEEPCLEAN));

        assertThat(created.getTaskId()).isEqualTo(TASK_ID);
        assertThat(created.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(created.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(created.getCompletedAt()).isNull();
        verify(auditRecorder).record("CREATE", "HOUSEKEEPINGTASK", TASK_ID);
    }

    /** Finishing a task stamps completedAt; only DONE earns that stamp. */
    @Test
    void updateTaskStatus_inprogressToDone() {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(TaskStatus.INPROGRESS)));
        when(taskRepository.save(any(HousekeepingTask.class))).thenAnswer(inv -> inv.getArgument(0));

        HousekeepingTaskResponse done = service.updateTaskStatus(TASK_ID, TaskStatus.DONE);

        assertThat(done.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(done.getCompletedAt()).isNotNull();
        verify(auditRecorder).record("UPDATE_STATUS", "HOUSEKEEPINGTASK", TASK_ID);
    }
}
