package com.staynest.room.entity;

import com.staynest.room.enums.RatePlanStatus;
import com.staynest.room.enums.RoomTypeName;
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
@Table(name = "room_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoomTypeID")
    private Integer roomTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Name", nullable = false)
    private RoomTypeName name;

    @Column(name = "BedConfiguration", length = 100)
    private String bedConfiguration;

    @Column(name = "MaxOccupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "BaseRate", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "AmenitiesList", columnDefinition = "TEXT")
    private String amenitiesList;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private RatePlanStatus status = RatePlanStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
