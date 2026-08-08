package com.staynest.reservation.serviceimpl;

import com.staynest.reservation.audit.AuditRecorder;
import com.staynest.reservation.client.IamServiceClient;
import com.staynest.reservation.dto.GuestProfileRequest;
import com.staynest.reservation.dto.GuestProfileResponse;
import com.staynest.reservation.entity.GuestProfile;
import com.staynest.reservation.enums.GuestStatus;
import com.staynest.reservation.enums.LoyaltyTier;
import com.staynest.reservation.exception.BadRequestException;
import com.staynest.reservation.repository.GuestProfileRepository;
import com.staynest.reservation.service.GuestUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guest profiles are keyed by email, because email is the join between a GuestID here and the
 * UserID an account is addressed by. One profile per email is therefore a hard rule.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuestProfileServiceImplTest {

    private static final int GUEST_ID = 4;
    private static final int USER_ID = 7;
    private static final String EMAIL = "asha@staynest.example";

    @Mock private AuditRecorder auditRecorder;
    @Mock private GuestProfileRepository guestProfileRepository;
    @Mock private IamServiceClient iamServiceClient;
    @Mock private GuestUserResolver guestUserResolver;
    @InjectMocks private GuestProfileServiceImpl service;

    private static GuestProfileRequest request() {
        GuestProfileRequest req = new GuestProfileRequest();
        req.setName("Asha Menon");
        req.setEmail(EMAIL);
        req.setPhone("9876543210");
        req.setNationality("Indian");
        return req;
    }

    private static GuestProfile guest() {
        GuestProfile gp = new GuestProfile();
        gp.setGuestId(GUEST_ID);
        gp.setName("Asha Menon");
        gp.setEmail(EMAIL);
        gp.setPhone("9876543210");
        gp.setUserId(USER_ID);
        gp.setStatus(GuestStatus.ACTIVE);
        gp.setLoyaltyTier(LoyaltyTier.NONE);
        return gp;
    }

    @Test
    void createProfile_valid() {
        when(guestProfileRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(guestUserResolver.lookupByEmail(EMAIL)).thenReturn(USER_ID);
        when(guestUserResolver.userIdFor(any(GuestProfile.class))).thenReturn(USER_ID);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(inv -> {
            GuestProfile gp = inv.getArgument(0);
            gp.setGuestId(GUEST_ID);
            return gp;
        });

        GuestProfileResponse created = service.createGuestProfile(request());

        assertThat(created.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(created.getEmail()).isEqualTo(EMAIL);
        // The account behind the profile is resolved and recorded up front, not guessed later.
        assertThat(created.getUserId()).isEqualTo(USER_ID);
        verify(auditRecorder).record("CREATE", "GUESTPROFILE", GUEST_ID);
    }

    @Test
    void getByEmail_valid() {
        when(guestProfileRepository.findByEmail(EMAIL)).thenReturn(Optional.of(guest()));
        when(guestUserResolver.userIdFor(any(GuestProfile.class))).thenReturn(USER_ID);

        GuestProfileResponse found = service.getGuestByEmail(EMAIL);

        assertThat(found.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(found.getName()).isEqualTo("Asha Menon");
    }

    @Test
    void duplicateEmail_throwsException() {
        when(guestProfileRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.createGuestProfile(request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already exists");

        verify(guestProfileRepository, never()).save(any());
    }
}
