package com.staynest.room.entity;

import com.staynest.room.enums.RatePlanName;
import com.staynest.room.enums.RatePlanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rate_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RatePlanID")
    private Integer ratePlanId;

    @ManyToOne
    @JoinColumn(name = "RoomTypeID", nullable = false)
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(name = "Name", nullable = false)
    private RatePlanName name;

    @Column(name = "PricePerNight", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "ValidFrom", nullable = false)
    private LocalDate validFrom;

    @Column(name = "ValidTo", nullable = false)
    private LocalDate validTo;

    @Column(name = "MealPlanIncluded")
    @Builder.Default
    private Boolean mealPlanIncluded = false;

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