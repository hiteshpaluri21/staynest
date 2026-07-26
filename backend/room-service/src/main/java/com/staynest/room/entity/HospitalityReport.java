package com.staynest.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospitality_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalityReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Integer reportId;

    @Column(name = "Scope", nullable = false, length = 50)
    private String scope;

    @Column(name = "Metrics", columnDefinition = "TEXT")
    private String metrics;

    @CreationTimestamp
    @Column(name = "GeneratedDate", updatable = false)
    private LocalDateTime generatedDate;
}