package com.staynest.reservation.repository;

import com.staynest.reservation.entity.GuestProfile;
import com.staynest.reservation.enums.GuestStatus;
import com.staynest.reservation.enums.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestProfileRepository extends JpaRepository<GuestProfile, Integer> {
    Optional<GuestProfile> findByEmail(String email);
    List<GuestProfile> findByStatus(GuestStatus status);
    List<GuestProfile> findByLoyaltyTier(LoyaltyTier loyaltyTier);
    boolean existsByEmail(String email);
}