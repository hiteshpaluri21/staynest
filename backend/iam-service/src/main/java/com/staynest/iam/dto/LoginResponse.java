package com.staynest.iam.dto;

import com.staynest.iam.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Role role;
    private Integer userId;
    private String email;
    private String name;
}