package com.staynest.iam.service;

import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.dto.UserResponse;
import com.staynest.iam.enums.UserStatus;

import java.util.List;
import com.staynest.iam.enums.Role;

public interface UserService {

    UserResponse createUser(UserRequest request);
    UserResponse getUserById(Integer id);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    List<UserResponse> getUsersByStatus(UserStatus status);
    List<UserResponse> getActiveUsersByRole(Role role);
    UserResponse updateUserStatus(Integer id, UserStatus status);
    void deleteUser(Integer id);
}