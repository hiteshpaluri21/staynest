package com.staynest.reservation.dto;

import com.staynest.reservation.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Payload for editing an existing guest profile. Email is immutable here (it identifies the
 * account), so unlike {@link GuestProfileRequest} it is not part of this request.
 */
@Data
public class GuestProfileUpdateRequest {
    @NotBlank
    private String name;
    @NotBlank
    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;
    private String nationality;
    private String idDocumentType;
    private String idNumber;
    private String preferencesJson;
}
