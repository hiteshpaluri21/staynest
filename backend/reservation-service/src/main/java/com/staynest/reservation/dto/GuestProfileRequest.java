package com.staynest.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestProfileRequest {
    @NotBlank
    private String name;
    @NotBlank @Email
    private String email;
    private String phone;
    private String nationality;
    private String idDocumentType;
    private String idNumber;
    private String preferencesJson;
}