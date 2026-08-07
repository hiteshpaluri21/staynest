package com.staynest.fb.entity;

import com.staynest.fb.enums.DiningResStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "dining_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiningResID")
    private Integer diningResId;

    @Column(name = "GuestID", nullable = false)
    private Integer guestId;

    @Column(name = "StayID")
    private Integer stayId;

    @Column(name = "RestaurantOutlet", nullable = false, length = 100)
    private String restaurantOutlet;

    @Column(name = "Date", nullable = false)
    private LocalDate date;

    @Column(name = "Time", nullable = false)
    private LocalTime time;

    /**
     * When the table is given up again. Nullable because rows written before end times
     * existed have none — readers fall back to a default sitting length.
     */
    @Column(name = "EndTime")
    private LocalTime endTime;

    @Column(name = "Covers", nullable = false)
    private Integer covers;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private DiningResStatus status = DiningResStatus.CONFIRMED;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}