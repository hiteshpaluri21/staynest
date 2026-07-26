package com.staynest.iam.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auditid")
    private Integer auditId;

    @Column(name = "userid")
    private Integer userId;

    @Column(name = "Action", nullable = false, length = 50)
    private String action;

    @Column(name = "EntityType", nullable = false, length = 50)
    private String entityType;

    @Column(name = "EntityId")
    private Integer entityId;

    @CreationTimestamp
    @Column(name = "Timestamp", updatable = false)
    private LocalDateTime timestamp;
}