package com.staynest.frontdesk.dto;

import com.staynest.frontdesk.enums.StayStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class StayRecordResponse {
    private Integer stayId;
    private Integer reservationId;
    private Integer guestId;
    private Integer assignedRoomId;
    private LocalDateTime actualCheckIn;
    private LocalDateTime actualCheckOut;
    private BigDecimal folioBalance;
    private StayStatus status;
}