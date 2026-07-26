package com.staynest.reservation.entity;

import com.staynest.reservation.enums.GuestStatus;
import com.staynest.reservation.enums.LoyaltyTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "guest_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GuestID")
    private Integer guestId;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "Nationality", length = 50)
    private String nationality;

    @Column(name = "IDDocumentType", length = 50)
    private String idDocumentType;

    @Column(name = "IDNumber", length = 100)
    private String idNumber;

    @Column(name = "PreferencesJSON", columnDefinition = "TEXT")
    private String preferencesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoyaltyTier")
    @Builder.Default
    private LoyaltyTier loyaltyTier = LoyaltyTier.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private GuestStatus status = GuestStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}