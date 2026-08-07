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

    /**
     * The iam-service user this profile belongs to, matched on email.
     *
     * GuestID and UserID are separate key spaces, and nothing recorded which guest was which
     * account — so anything addressed to a guest (notifications, above all) had to guess, and
     * guessed by passing the GuestID as a UserID. Nullable: a walk-in profile created by staff
     * has no login, and rows predating this column are filled in on first read.
     */
    @Column(name = "UserID")
    private Integer userId;

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