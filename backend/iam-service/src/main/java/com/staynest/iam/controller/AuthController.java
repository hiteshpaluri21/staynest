package com.staynest.iam.controller;

import com.staynest.iam.dto.ApiResponse;
import com.staynest.iam.dto.LoginRequest;
import com.staynest.iam.dto.LoginResponse;
import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse session = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", session));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody UserRequest request) {
        LoginResponse session = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", session));
    }
}
