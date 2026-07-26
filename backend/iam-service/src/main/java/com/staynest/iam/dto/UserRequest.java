package com.staynest.iam.dto;

import com.staynest.iam.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank
    private String name;
    @NotNull
    private Role role;
    @NotBlank @Email
    private String email;
    private String phone;
    @NotBlank
    private String password;
}
