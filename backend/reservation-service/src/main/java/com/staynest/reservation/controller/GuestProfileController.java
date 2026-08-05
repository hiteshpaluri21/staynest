package com.staynest.reservation.controller;

import com.staynest.reservation.dto.ApiResponse;
import com.staynest.reservation.dto.GuestProfileRequest;
import com.staynest.reservation.dto.GuestProfileResponse;
import com.staynest.reservation.dto.GuestProfileUpdateRequest;
import com.staynest.reservation.enums.LoyaltyTier;
import com.staynest.reservation.service.GuestProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestProfileController {

    private final GuestProfileService guestProfileService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK')")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> create(@Valid @RequestBody GuestProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Guest created", guestProfileService.createGuestProfile(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK')")
    public ResponseEntity<ApiResponse<List<GuestProfileResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(guestProfileService.getAllGuests()));
    }

    /**
     * The caller's own guest profile, resolved from the JWT. Clients must use the guestId from
     * here rather than their IAM userId — the two are different keys in different services.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(guestProfileService.getOrCreateCurrentGuest()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(guestProfileService.getGuestById(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.success(guestProfileService.getGuestByEmail(email)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'GUEST')")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody GuestProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Guest updated", guestProfileService.updateGuestProfile(id, request)));
    }

    @PatchMapping("/{id}/loyalty")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> updateLoyalty(
            @PathVariable Integer id,
            @RequestParam LoyaltyTier tier) {
        return ResponseEntity.ok(ApiResponse.success(guestProfileService.updateLoyaltyTier(id, tier)));
    }

    @PatchMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GuestProfileResponse>> blacklist(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(guestProfileService.blacklistGuest(id)));
    }
}