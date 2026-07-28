package com.staynest.reservation.serviceimpl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GuestProfileServiceImpl implements GuestProfileService {

    private static final Logger log = LoggerFactory.getLogger(GuestProfileServiceImpl.class);

    @Autowired
    private GuestProfileRepository guestProfileRepository;

    @Override
    public GuestProfileResponse createGuestProfile(GuestProfileRequest request) {
        if (guestProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        GuestProfile guest = new GuestProfile();
        guest.setName(request.getName());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());
        guest.setNationality(request.getNationality());
        guest.setIdDocumentType(request.getIdDocumentType());
        guest.setIdNumber(request.getIdNumber());
        guest.setPreferencesJson(request.getPreferencesJson());

        GuestProfile saved = guestProfileRepository.save(guest);
        log.info("GuestProfile created: {}", saved.getGuestId());
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
        return mapToResponse(updated);
    }

    @Override
    public List<GuestProfileResponse> getAllGuests() {
        return guestProfileRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public GuestProfileResponse updateLoyaltyTier(Integer id, LoyaltyTier tier) {
        GuestProfile guest = guestProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + id));
        guest.setLoyaltyTier(tier);
        GuestProfile updated = guestProfileRepository.save(guest);
        log.info("Guest {} loyalty tier updated to {}", id, tier);
        return mapToResponse(updated);
    }

    @Override
    public GuestProfileResponse blacklistGuest(Integer id) {
        GuestProfile guest = guestProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + id));
        guest.setStatus(GuestStatus.BLACKLISTED);
        GuestProfile updated = guestProfileRepository.save(guest);
        log.info("Guest {} blacklisted", id);
        return mapToResponse(updated);
    }

    private GuestProfileResponse mapToResponse(GuestProfile guest) {
        GuestProfileResponse response = new GuestProfileResponse();
        response.setGuestId(guest.getGuestId());
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