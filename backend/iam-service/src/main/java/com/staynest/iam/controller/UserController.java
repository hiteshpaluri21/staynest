package com.staynest.iam.controller;

import com.staynest.iam.dto.ApiResponse;
import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.dto.UserResponse;
import com.staynest.iam.enums.UserStatus;
import com.staynest.iam.exception.BadRequestException;
import com.staynest.iam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.staynest.iam.enums.Role;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        log.info("Creating user: {}", request.getEmail());
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(email)));
    }

    // Open endpoint (like /email/{email}) so other services can resolve staff recipients
    // for notifications (e.g. all FRONTDESK / HOUSEKEEPING users). Returns ACTIVE users only.
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(
            @PathVariable Role role) {
        return ResponseEntity.ok(ApiResponse.success(userService.getActiveUsersByRole(role)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam UserStatus status,
            Authentication authentication) {
        if (status == UserStatus.INACTIVE && isSelf(authentication, id)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        log.info("Updating user {} status to {}", id, status);
        return ResponseEntity.ok(ApiResponse.success(userService.updateUserStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id, Authentication authentication) {
        if (isSelf(authentication, id)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        log.info("Deleting user: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /** The JWT subject is the caller's email; resolve it to the user id to detect self-targeting. */
    private boolean isSelf(Authentication authentication, Integer targetId) {
        if (authentication == null || authentication.getName() == null) {
            return false;
        }
        UserResponse caller = userService.getUserByEmail(authentication.getName());
        return caller != null && targetId.equals(caller.getUserId());
    }
}