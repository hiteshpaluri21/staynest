package com.staynest.iam.controller;

import com.staynest.iam.dto.ApiResponse;
import com.staynest.iam.dto.LoginRequest;
import com.staynest.iam.dto.LoginResponse;
import com.staynest.iam.entity.User;
import com.staynest.iam.repository.UserRepository;
import com.staynest.iam.config.JwtUtil;
import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.dto.UserResponse;
import com.staynest.iam.service.AuditLogService;
import com.staynest.iam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.staynest.iam.enums.Role;
import com.staynest.iam.enums.UserStatus;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid credentials for: {}", request.getEmail());
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid email or password"));
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login attempt for deactivated user: {}", request.getEmail());
            return ResponseEntity.status(403).body(ApiResponse.error("Account is deactivated. Please contact an administrator."));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
        
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .role(user.getRole())
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();

        /*
         * Successful sign-ins go on the trail so an admin can see who has been in the
         * system. Rejected attempts are deliberately not recorded — a wrong password
         * would otherwise let anyone who knows an email address fill the table.
         */
        auditLogService.logAction(user.getUserId(), "LOGIN", "User", user.getUserId());

        log.info("Login successful for: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody UserRequest request) {
        log.info("Registration attempt for: {}", request.getEmail());
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(400).body(ApiResponse.error("Email is already registered. Please log in."));
        }

        // Public self-registration must never let a client choose a privileged role.
        // Force GUEST regardless of what the request body supplies.
        request.setRole(Role.GUEST);

        UserResponse created = userService.createUser(request);
        String token = jwtUtil.generateToken(created.getEmail(), created.getRole().name(), created.getUserId());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .role(created.getRole())
                .userId(created.getUserId())
                .email(created.getEmail())
                .name(created.getName())
                .build();

        log.info("Registration successful for: {}", created.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registration successful", response));
    }
}