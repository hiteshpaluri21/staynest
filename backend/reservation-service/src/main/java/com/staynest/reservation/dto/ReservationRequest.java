package com.staynest.reservation.dto;

import com.staynest.reservation.enums.BookingChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReservationRequest {
    @NotNull
    private Integer guestId;
    @NotNull
    private Integer roomTypeId;
    private Integer ratePlanId;
    @NotNull
    private LocalDate checkInDate;
    @NotNull
    private LocalDate checkOutDate;
    @NotNull @Positive
    private Integer nights;
    @NotNull @Positive
    private Integer adults;
    @PositiveOrZero
    private Integer children;
    @NotNull @Positive
    private BigDecimal totalAmount;
    private BookingChannel bookingChannel;
}