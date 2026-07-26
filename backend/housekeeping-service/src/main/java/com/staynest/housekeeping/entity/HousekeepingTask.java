package com.staynest.housekeeping.entity;

import com.staynest.housekeeping.enums.TaskStatus;
import com.staynest.housekeeping.enums.TaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "housekeeping_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousekeepingTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TaskID")
    private Integer taskId;

    @Column(name = "RoomID", nullable = false)
    private Integer roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TaskType", nullable = false)
    private TaskType taskType;

    @Column(name = "AssignedToID")
    private Integer assignedToId;

    @Column(name = "AssignedDate")
    private LocalDate assignedDate;

    @Column(name = "CompletedAt")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}