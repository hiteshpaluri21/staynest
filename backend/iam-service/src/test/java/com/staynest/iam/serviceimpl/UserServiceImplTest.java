package com.staynest.iam.serviceimpl;

import com.staynest.iam.dto.UserRequest;
import com.staynest.iam.dto.UserResponse;
import com.staynest.iam.entity.User;
import com.staynest.iam.enums.Role;
import com.staynest.iam.enums.UserStatus;
import com.staynest.iam.exception.BadRequestException;
import com.staynest.iam.exception.ResourceNotFoundException;
import com.staynest.iam.repository.UserRepository;
import com.staynest.iam.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Account creation, lookup and deactivation.
 *
 * The rules worth pinning down are that a password never reaches the database in the clear,
 * that an email can only be used once, and that a missing account is reported rather than
 * returned as null.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    private static final int USER_ID = 7;
    private static final String EMAIL = "asha@staynest.example";
    private static final String RAW_PASSWORD = "Str0ng!pass";
    private static final String HASHED_PASSWORD = "$2a$10$hashed";

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private UserServiceImpl service;

    private static UserRequest request() {
        UserRequest req = new UserRequest();
        req.setName("Asha Menon");
        req.setEmail(EMAIL);
        req.setPhone("9876543210");
        req.setPassword(RAW_PASSWORD);
        req.setRole(Role.GUEST);
        return req;
    }

    private static User user(UserStatus status) {
        User u = new User();
        u.setUserId(USER_ID);
        u.setName("Asha Menon");
        u.setEmail(EMAIL);
        u.setPhone("9876543210");
        u.setPassword(HASHED_PASSWORD);
        u.setRole(Role.GUEST);
        u.setStatus(status);
        return u;
    }

    /** The repository hands back what it was asked to save, with an id, as a real one would. */
    private void savesWhatItIsGiven() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(USER_ID);
            return u;
        });
    }

    @Test
    void createUser_valid() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        savesWhatItIsGiven();

        UserResponse created = service.createUser(request());

        assertThat(created.getUserId()).isEqualTo(USER_ID);
        assertThat(created.getEmail()).isEqualTo(EMAIL);
        assertThat(created.getRole()).isEqualTo(Role.GUEST);
        // A new account starts usable, and the password is stored hashed, never as sent.
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(auditLogService).logAction(USER_ID, "CREATE_USER", "User", USER_ID);
    }

    @Test
    void duplicateEmail_throwsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_valid() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));

        UserResponse found = service.getUserById(USER_ID);

        assertThat(found.getUserId()).isEqualTo(USER_ID);
        assertThat(found.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void getUserById_invalid_throwsException() {
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(404))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 404");
    }

    @Test
    void updateStatus() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse updated = service.updateUserStatus(USER_ID, UserStatus.INACTIVE);

        assertThat(updated.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(auditLogService).logAction(USER_ID, "UPDATE_STATUS", "User", USER_ID);
    }
}
