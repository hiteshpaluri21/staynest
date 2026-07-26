package com.staynest.reservation.dto;

import com.staynest.reservation.enums.BookingChannel;
import com.staynest.reservation.enums.ReservationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ReservationResponse {
    private Integer reservationId;
    private Integer guestId;
    private String guestName;
    private Integer roomTypeId;
    private Integer ratePlanId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer adults;
    private Integer children;
    private BigDecimal totalAmount;
    private BookingChannel bookingChannel;
    private ReservationStatus status;
}