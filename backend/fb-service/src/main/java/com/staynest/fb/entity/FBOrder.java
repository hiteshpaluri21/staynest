package com.staynest.fb.entity;

import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fb_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FBOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @Column(name = "StayID", nullable = false)
    private Integer stayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "OrderType", nullable = false)
    private OrderType orderType;

    @Column(name = "ItemsJSON", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "TotalAmount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** Who placed the order — attributes the folio charge and its reversal on cancellation. */
    @Column(name = "PlacedBy")
    private Integer placedBy;

    @CreationTimestamp
    @Column(name = "OrderTime", updatable = false)
    private LocalDateTime orderTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}