package com.staynest.housekeeping.entity;

import com.staynest.housekeeping.enums.MaintenancePriority;
import com.staynest.housekeeping.enums.MaintenanceStatus;
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
@Table(name = "maintenance_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Integer requestId;

    @Column(name = "RoomID", nullable = false)
    private Integer roomId;

    @Column(name = "ReportedBy", nullable = false)
    private Integer reportedBy;

    @Column(name = "IssueDescription", columnDefinition = "TEXT")
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "Priority", nullable = false)
    private MaintenancePriority priority;

    @Column(name = "RaisedDate")
    @Builder.Default
    private LocalDate raisedDate = LocalDate.now();

    @Column(name = "ResolvedDate")
    private LocalDate resolvedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}