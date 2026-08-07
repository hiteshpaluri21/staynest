package com.staynest.fb.dto;

import com.staynest.fb.enums.DiningResStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class DiningReservationResponse {
    private Integer diningResId;
    private Integer guestId;
    private Integer stayId;
    private String restaurantOutlet;
    private LocalDate date;
    private LocalTime time;
    private LocalTime endTime;
    private Integer covers;
    private DiningResStatus status;
}