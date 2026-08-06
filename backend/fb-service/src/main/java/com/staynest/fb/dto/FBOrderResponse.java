package com.staynest.fb.dto;

import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.enums.OrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FBOrderResponse {
    private Integer orderId;
    private Integer stayId;
    private OrderType orderType;
    private String itemsJson;
    private List<FBOrderItemResponse> items;
    private BigDecimal totalAmount;
    private LocalDateTime orderTime;
    private OrderStatus status;
}