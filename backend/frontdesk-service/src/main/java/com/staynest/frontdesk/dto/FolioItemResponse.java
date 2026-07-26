package com.staynest.frontdesk.dto;

import com.staynest.frontdesk.enums.ChargeType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FolioItemResponse {
    private Integer folioItemId;
    private Integer stayId;
    private ChargeType chargeType;
    private String description;
    private BigDecimal amount;
    private LocalDateTime postedDate;
    private Integer postedBy;
}