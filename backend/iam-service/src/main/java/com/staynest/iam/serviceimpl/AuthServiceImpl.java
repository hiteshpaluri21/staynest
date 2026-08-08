package com.staynest.iam.serviceimpl;

import com.staynest.iam.config.JwtUtil;
import com.staynest.iam.dto.LoginRequest;
import com.staynest.iam.dto.LoginResponse;
import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.dto.UserResponse;
import com.staynest.iam.entity.User;
import com.staynest.iam.enums.Role;
import com.staynest.iam.enums.UserStatus;
import com.staynest.iam.exception.BadRequestException;
import com.staynest.iam.exception.ForbiddenException;
import com.staynest.iam.exception.UnauthorizedException;
import com.staynest.iam.repository.UserRepository;
import com.staynest.iam.service.AuditLogService;
import com.staynest.iam.service.AuthService;
import com.staynest.iam.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-in and public self-registration.
 *
 * Both flows end the same way: a verified user turns into a signed JWT plus the
 * handful of fields the client needs to render a session.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        /*
         * A missing account and a wrong password are answered identically, so the
         * response cannot be used to work out which email addresses are registered.
         */
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid credentials for: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login attempt for deactivated user: {}", request.getEmail());
            throw new ForbiddenException("Account is deactivated. Please contact an administrator.");
        }

        /*
         * Successful sign-ins go on the trail so an admin can see who has been in the
         * system. Rejected attempts are deliberately not recorded — a wrong password
         * would otherwise let anyone who knows an email address fill the table.
         */
        auditLogService.logAction(user.getUserId(), "LOGIN", "User", user.getUserId());

        log.info("Login successful for: {}", request.getEmail());
        return buildSession(user.getUserId(), user.getEmail(), user.getName(), user.getRole());
    }

    @Override
    @Transactional
    public LoginResponse register(UserRequest request) {
        log.info("Registration attempt for: {}", request.getEmail());

        // createUser reports this as "Email already exists"; a visitor at the register
        // form is better served by being pointed at the sign-in page.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered. Please log in.");
        }

        // Public self-registration must never let a client choose a privileged role.
        // Force GUEST regardless of what the request body supplies.
        request.setRole(Role.GUEST);

        UserResponse created = userService.createUser(request);

        log.info("Registration successful for: {}", created.getEmail());
        return buildSession(created.getUserId(), created.getEmail(), created.getName(), created.getRole());
    }

    /** Mints the token and wraps it with the fields the client keeps for the session. */
    private LoginResponse buildSession(Integer userId, String email, String name, Role role) {
        return LoginResponse.builder()
                .token(jwtUtil.generateToken(email, role.name(), userId))
                .role(role)
                .userId(userId)
                .email(email)
                .name(name)
                .build();
    }
}
