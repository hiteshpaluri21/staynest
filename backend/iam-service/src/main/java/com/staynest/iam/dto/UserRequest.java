package com.staynest.iam.dto;

import com.staynest.iam.enums.Role;
import com.staynest.iam.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank
    private String name;
    @NotNull
    private Role role;
    @NotBlank
    @Email(message = "must be a valid email address (e.g. john@example.com)")
    @Size(max = 150, message = "must be 150 characters or fewer")
    private String email;
    @NotBlank
    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;
    @NotBlank
    @Size(min = 6, message = "must be at least 6 characters")
    private String password;
}
