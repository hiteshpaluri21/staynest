package com.staynest.reservation.serviceimpl;

import com.staynest.reservation.audit.AuditRecorder;
import com.staynest.reservation.dto.GuestProfileRequest;
import com.staynest.reservation.dto.GuestProfileResponse;
import com.staynest.reservation.dto.GuestProfileUpdateRequest;
import com.staynest.reservation.entity.GuestProfile;
import com.staynest.reservation.enums.GuestStatus;
import com.staynest.reservation.enums.LoyaltyTier;
import com.staynest.reservation.exception.BadRequestException;
import com.staynest.reservation.exception.ResourceNotFoundException;
import com.staynest.reservation.repository.GuestProfileRepository;
import com.staynest.reservation.service.GuestProfileService;
import com.staynest.reservation.service.GuestUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.staynest.reservation.client.IamServiceClient;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class GuestProfileServiceImpl implements GuestProfileService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "GUESTPROFILE";

    @Autowired
    private AuditRecorder auditRecorder;

    private static final Logger log = LoggerFactory.getLogger(GuestProfileServiceImpl.class);

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    /** Optional so the service still starts if iam-service is unreachable. */
    @Autowired(required = false)
    private IamServiceClient iamServiceClient;

    @Autowired
    private GuestUserResolver guestUserResolver;

    @Override
    @Transactional
    public GuestProfileResponse getOrCreateCurrentGuest() {
        String email = currentUserEmail();
        if (email == null) {
            throw new BadRequestException("No authenticated user to resolve a guest profile for.");
        }

        // Registration stores name/phone on the IAM user, not here, so carry them across rather
        // than making the guest type their phone number a second time when they book.
        Map<String, Object> iamUser = fetchIamUser(email);

        GuestProfile guest = guestProfileRepository.findByEmail(email).orElseGet(() -> {
            GuestProfile gp = new GuestProfile();
            gp.setName(valueOf(iamUser, "name", localPart(email)));
            gp.setEmail(email);
            gp.setPhone(valueOf(iamUser, "phone", null));
            // The account is right here in the payload, so the link is recorded up front.
            gp.setUserId(intValueOf(iamUser, "userId"));
            gp.setStatus(GuestStatus.ACTIVE);
            gp.setLoyaltyTier(LoyaltyTier.NONE);
            log.info("Provisioning GuestProfile for authenticated user {}", email);
            return guestProfileRepository.save(gp);
        });

        // Back-fill for profiles created before this ran (or created by the booking path).
        if (isBlank(guest.getPhone())) {
            String iamPhone = valueOf(iamUser, "phone", null);
            if (!isBlank(iamPhone)) {
                guest.setPhone(iamPhone);
                guest = guestProfileRepository.save(guest);
                log.info("Back-filled phone for guest {} from iam-service", guest.getGuestId());
            }
        }
        return mapToResponse(guest);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String localPart(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    /** Reads a numeric field from the IAM user payload, or null when absent. */
    private static Integer intValueOf(Map<String, Object> user, String key) {
        Object v = user != null ? user.get(key) : null;
        return v instanceof Number n ? n.intValue() : null;
    }

    /** Reads a string field from the IAM user payload, falling back when absent or blank. */
    private static String valueOf(Map<String, Object> user, String key, String fallback) {
        if (user == null) {
            return fallback;
        }
        Object v = user.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : fallback;
    }

    /** Best-effort IAM user lookup; returns null if iam-service is unreachable. */
    private Map<String, Object> fetchIamUser(String email) {
        try {
            if (iamServiceClient != null) {
                var resp = iamServiceClient.getUserByEmail(email);
                if (resp != null && resp.getData() != null) {
                    return resp.getData();
                }
            }
        } catch (Exception e) {
            log.warn("Could not load iam-service user for {}: {}", email, e.getMessage());
        }
        return null;
    }

    /** The JWT subject is the user's email (see JwtUtil / JwtFilter). */
    private String currentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return auth.getName();
    }


    @Override
    @Transactional
    public GuestProfileResponse createGuestProfile(GuestProfileRequest request) {
        if (guestProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        GuestProfile guest = new GuestProfile();
        guest.setName(request.getName());
        guest.setEmail(request.getEmail());
        // Null for a walk-in with no login — they simply get no notifications.
        guest.setUserId(guestUserResolver.lookupByEmail(request.getEmail()));
        guest.setPhone(request.getPhone());
        guest.setNationality(request.getNationality());
        guest.setIdDocumentType(request.getIdDocumentType());
        guest.setIdNumber(request.getIdNumber());
        guest.setPreferencesJson(request.getPreferencesJson());

        GuestProfile saved = guestProfileRepository.save(guest);
        log.info("GuestProfile created: {}", saved.getGuestId());
        auditRecorder.record("CREATE", ENTITY, saved.getGuestId());
        return mapToResponse(saved);
    }

    @Override
    public GuestProfileResponse getGuestById(Integer id) {
        GuestProfile guest = guestProfileRepository.findById(id)
                .orElseGet(() -> {
                    String email = "guest" + id + "@staynest.com";
                    return guestProfileRepository.findByEmail(email)
                            .orElseGet(() -> {
                                log.info("GuestProfile {} not found, auto-creating default profile", id);
                                GuestProfile gp = new GuestProfile();
                                gp.setName("User #" + id);
                                gp.setEmail(email);
                                gp.setStatus(GuestStatus.ACTIVE);
                                gp.setLoyaltyTier(LoyaltyTier.NONE);
                                return guestProfileRepository.save(gp);
                            });
                });
        return mapToResponse(guest);
    }

    @Override
    public GuestProfileResponse getGuestByEmail(String email) {
        GuestProfile guest = guestProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with email: " + email));
        return mapToResponse(guest);
    }

    @Override
    @Transactional
    public GuestProfileResponse updateGuestProfile(Integer id, GuestProfileUpdateRequest request) {
        GuestProfile guest = guestProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + id));
        // Only overwrite fields the client actually supplied, so a partial edit can't null out
        // stored data (name is @NotBlank so it is always present).
        guest.setName(request.getName());
        if (request.getPhone() != null) guest.setPhone(request.getPhone());
        if (request.getNationality() != null) guest.setNationality(request.getNationality());
        if (request.getIdDocumentType() != null) guest.setIdDocumentType(request.getIdDocumentType());
        if (request.getIdNumber() != null) guest.setIdNumber(request.getIdNumber());
        if (request.getPreferencesJson() != null) guest.setPreferencesJson(request.getPreferencesJson());
        GuestProfile updated = guestProfileRepository.save(guest);
        log.info("Guest {} profile updated", id);
        auditRecorder.record("UPDATE", ENTITY, id);
        return mapToResponse(updated);
    }

    @Override
    public List<GuestProfileResponse> getAllGuests() {
        return guestProfileRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GuestProfileResponse updateLoyaltyTier(Integer id, LoyaltyTier tier) {
        GuestProfile guest = guestProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + id));
        guest.setLoyaltyTier(tier);
        GuestProfile updated = guestProfileRepository.save(guest);
        log.info("Guest {} loyalty tier updated to {}", id, tier);
        auditRecorder.record("UPDATE_LOYALTY", ENTITY, id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public GuestProfileResponse blacklistGuest(Integer id) {
        GuestProfile guest = guestProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + id));
        guest.setStatus(GuestStatus.BLACKLISTED);
        GuestProfile updated = guestProfileRepository.save(guest);
        log.info("Guest {} blacklisted", id);
        auditRecorder.record("BLACKLIST", ENTITY, id);
        return mapToResponse(updated);
    }

    private GuestProfileResponse mapToResponse(GuestProfile guest) {
        GuestProfileResponse response = new GuestProfileResponse();
        response.setGuestId(guest.getGuestId());
        // Resolved and cached here, so every service reading a guest gets the account to
        // address without having to know that email is the join between the two key spaces.
        response.setUserId(guestUserResolver.userIdFor(guest));
        response.setName(guest.getName());
        response.setEmail(guest.getEmail());
        response.setPhone(guest.getPhone());
        response.setNationality(guest.getNationality());
        response.setIdDocumentType(guest.getIdDocumentType());
        response.setIdNumber(guest.getIdNumber());
        response.setPreferencesJson(guest.getPreferencesJson());
        response.setLoyaltyTier(guest.getLoyaltyTier());
        response.setStatus(guest.getStatus());
        return response;
    }
}