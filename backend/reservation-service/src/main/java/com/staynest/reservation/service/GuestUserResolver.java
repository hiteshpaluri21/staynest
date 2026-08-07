package com.staynest.reservation.service;

import com.staynest.reservation.client.IamServiceClient;
import com.staynest.reservation.entity.GuestProfile;
import com.staynest.reservation.repository.GuestProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps a guest profile to the iam-service account behind it.
 *
 * GuestID and UserID are different key spaces. Everything that reaches a guest — notifications
 * especially — is addressed by UserID, so passing a GuestID delivered to whichever unrelated
 * account happened to share that number, and the guest themselves got nothing. Email is unique
 * on both sides, so it is the join.
 *
 * The answer is cached on the guest row: the lookup happens once per profile, not once per
 * notification, and profiles created before the column existed are filled in on first use.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuestUserResolver {

    private final GuestProfileRepository guestProfileRepository;

    /** Optional so the service still starts when iam-service is unreachable. */
    @Autowired(required = false)
    private IamServiceClient iamServiceClient;

    /**
     * The IAM userId behind this guest, or null when there is no matching account — a walk-in
     * profile staff typed in, or iam-service being unreachable. Callers treat null as "nobody
     * to notify" rather than falling back to the guestId, which is what caused the mis-delivery.
     */
    public Integer userIdFor(GuestProfile guest) {
        if (guest == null) {
            return null;
        }
        if (guest.getUserId() != null) {
            return guest.getUserId();
        }
        Integer resolved = lookupByEmail(guest.getEmail());
        if (resolved == null) {
            return null;
        }
        guest.setUserId(resolved);
        guestProfileRepository.save(guest);
        log.info("Linked guest {} to iam-service user {}", guest.getGuestId(), resolved);
        return resolved;
    }

    /** Best-effort lookup used when creating a profile, before it has been saved. */
    public Integer lookupByEmail(String email) {
        if (email == null || email.isBlank() || iamServiceClient == null) {
            return null;
        }
        try {
            var resp = iamServiceClient.getUserByEmail(email);
            Map<String, Object> user = resp != null ? resp.getData() : null;
            Object id = user != null ? user.get("userId") : null;
            return id instanceof Number n ? n.intValue() : null;
        } catch (Exception e) {
            // A guest with no login is normal, so this is not an error.
            log.debug("No iam-service user for guest email {}: {}", email, e.getMessage());
            return null;
        }
    }
}
