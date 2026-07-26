package com.staynest.reservation.entity;

import com.staynest.reservation.enums.BookingChannel;
import com.staynest.reservation.enums.ReservationStatus;
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
@Table(name = "reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReservationID")
    private Integer reservationId;

    @ManyToOne
    @JoinColumn(name = "GuestID", nullable = false)
    private GuestProfile guest;

    @Column(name = "RoomTypeID", nullable = false)
    private Integer roomTypeId;

    @Column(name = "RatePlanID", nullable = false)
    private Integer ratePlanId;

    @Column(name = "CheckInDate", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "CheckOutDate", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "Nights", nullable = false)
    private Integer nights;

    @Column(name = "Adults", nullable = false)
    private Integer adults;

    @Column(name = "Children")
    private Integer children;

    @Column(name = "TotalAmount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "BookingChannel")
    @Builder.Default
    private BookingChannel bookingChannel = BookingChannel.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}