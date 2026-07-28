package com.staynest.fb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single, resolved line of an F&B order. Derived from the stored {@code itemsJson}
 * so clients can render item names and quantities instead of a raw JSON blob.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FBOrderItemResponse {
    private Integer menuItemId;
    private String name;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
