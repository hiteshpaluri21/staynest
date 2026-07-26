package com.staynest.iam.controller;

import com.staynest.iam.dto.ApiResponse;
import com.staynest.iam.dto.LoginRequest;
import com.staynest.iam.dto.LoginResponse;
import com.staynest.iam.entity.User;
import com.staynest.iam.config.JwtUtil;
import com.staynest.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid credentials for: {}", request.getEmail());
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid email or password"));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .role(user.getRole())
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();

        log.info("Login successful for: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}