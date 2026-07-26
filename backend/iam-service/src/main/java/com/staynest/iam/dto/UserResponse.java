package com.staynest.iam.dto;

import com.staynest.iam.enums.Role;
import com.staynest.iam.enums.UserStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserResponse {
    
	private Integer userId;
    private String name;
    private Role role;
    private String email;
    private String phone;
    private UserStatus status;
    private LocalDateTime createdAt;
}
