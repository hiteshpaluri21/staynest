package com.staynest.iam.service;

import com.staynest.iam.dto.LoginRequest;
import com.staynest.iam.dto.LoginResponse;
import com.staynest.iam.dto.UserRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    LoginResponse register(UserRequest request);
}
