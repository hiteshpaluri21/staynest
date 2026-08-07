package com.staynest.fb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DiningReservationRequest {
    @NotNull
    private Integer guestId;
    private Integer stayId;
    @NotBlank
    private String restaurantOutlet;
    @NotNull
    private LocalDate date;
    @NotNull
    private LocalTime time;
    /** Optional — defaults to a standard sitting after {@code time} when the caller omits it. */
    private LocalTime endTime;
    @NotNull @Positive
    private Integer covers;
}