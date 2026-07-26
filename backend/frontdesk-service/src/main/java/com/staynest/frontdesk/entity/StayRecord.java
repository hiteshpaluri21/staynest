package com.staynest.frontdesk.entity;

import com.staynest.frontdesk.enums.StayStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stay_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StayRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StayID")
    private Integer stayId;

    @Column(name = "ReservationID", nullable = false, unique = true)
    private Integer reservationId;

    @Column(name = "GuestID", nullable = false)
    private Integer guestId;

    @Column(name = "AssignedRoomID", nullable = false)
    private Integer assignedRoomId;

    @Column(name = "ActualCheckIn")
    private LocalDateTime actualCheckIn;

    @Column(name = "ActualCheckOut")
    private LocalDateTime actualCheckOut;

    @Column(name = "FolioBalance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal folioBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private StayStatus status = StayStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}