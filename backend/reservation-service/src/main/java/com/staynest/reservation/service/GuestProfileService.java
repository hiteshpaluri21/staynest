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
    GuestProfileResponse updateGuestProfile(Integer id, GuestProfileUpdateRequest request);
    List<GuestProfileResponse> getAllGuests();
    GuestProfileResponse updateLoyaltyTier(Integer id, LoyaltyTier tier);
    GuestProfileResponse blacklistGuest(Integer id);
}