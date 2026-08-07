package com.staynest.reservation.dto;

import com.staynest.reservation.enums.GuestStatus;
import com.staynest.reservation.enums.LoyaltyTier;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GuestProfileResponse {
    private Integer guestId;
    /** The iam-service account behind this guest, so other services can address them. */
    private Integer userId;
    private String name;
    private String email;
    private String phone;
    private String nationality;
    private String idDocumentType;
    private String idNumber;
    private String preferencesJson;
    private LoyaltyTier loyaltyTier;
    private GuestStatus status;
}