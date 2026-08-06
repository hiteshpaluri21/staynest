package com.staynest.iam.serviceimpl;

import com.staynest.iam.audit.CurrentUser;
import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.dto.UserResponse;
import com.staynest.iam.entity.User;
import com.staynest.iam.enums.UserStatus;
import com.staynest.iam.exception.BadRequestException;
import com.staynest.iam.exception.ResourceNotFoundException;
import com.staynest.iam.repository.UserRepository;
import com.staynest.iam.service.AuditLogService;
import com.staynest.iam.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.staynest.iam.enums.Role;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Who to record against a user change: the admin performing it, falling back to the
     * affected account when nobody is authenticated — which is public self-registration
     * creating its own row.
     */
    private Integer actorFor(Integer affectedUserId) {
        Integer actor = CurrentUser.id();
        return actor != null ? actor : affectedUserId;
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        auditLogService.logAction(actorFor(saved.getUserId()), "CREATE_USER", "User", saved.getUserId());
        log.info("User created: {}", saved.getEmail());
        return mapToResponse(saved);
    }

    @Override
    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getActiveUsersByRole(Role role) {
        return userRepository.findByRoleAndStatus(role, UserStatus.ACTIVE).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Integer id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        User updated = userRepository.save(user);
        auditLogService.logAction(actorFor(updated.getUserId()), "UPDATE_STATUS", "User", updated.getUserId());
        log.info("User {} status updated to {}", id, status);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        auditLogService.logAction(actorFor(id), "SOFT_DELETE", "User", id);
        log.info("User {} soft deleted", id);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}