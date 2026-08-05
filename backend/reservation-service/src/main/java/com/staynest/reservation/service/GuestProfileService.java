package com.staynest.reservation.service;

import com.staynest.reservation.dto.GuestProfileRequest;
import com.staynest.reservation.dto.GuestProfileResponse;
import com.staynest.reservation.dto.GuestProfileUpdateRequest;
import com.staynest.reservation.enums.LoyaltyTier;

import java.util.List;

public interface GuestProfileService {

    GuestProfileResponse createGuestProfile(GuestProfileRequest request);
    GuestProfileResponse getGuestById(Integer id);
    GuestProfileResponse getGuestByEmail(String email);

    /**
     * Resolves the GuestProfile belonging to the currently authenticated user (by the JWT's
     * email), provisioning one if it does not exist yet. This is the only reliable way for a
     * client to learn its own guestId, which is NOT the same as the IAM userId.
     */
    GuestProfileResponse getOrCreateCurrentGuest();
    GuestProfileResponse updateGuestProfile(Integer id, GuestProfileUpdateRequest request);
    List<GuestProfileResponse> getAllGuests();
    GuestProfileResponse updateLoyaltyTier(Integer id, LoyaltyTier tier);
    GuestProfileResponse blacklistGuest(Integer id);
}