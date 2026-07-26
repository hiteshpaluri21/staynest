package com.staynest.frontdesk.entity;

import com.staynest.frontdesk.enums.ChargeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "folio_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FolioItemID")
    private Integer folioItemId;

    @ManyToOne
    @JoinColumn(name = "StayID", nullable = false)
    private StayRecord stayRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "ChargeType", nullable = false)
    private ChargeType chargeType;

    @Column(name = "Description", length = 200)
    private String description;

    @Column(name = "Amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "PostedDate", updatable = false)
    private LocalDateTime postedDate;

    @Column(name = "PostedBy")
    private Integer postedBy;
}