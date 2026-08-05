package com.staynest.reservation.dto;

import com.staynest.reservation.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuestProfileRequest {
    @NotBlank
    private String name;
    @NotBlank
    @Email(message = "must be a valid email address (e.g. john@example.com)")
    @Size(max = 150, message = "must be 150 characters or fewer")
    private String email;
    @NotBlank
    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;
    private String nationality;
    private String idDocumentType;
    private String idNumber;
    private String preferencesJson;
}