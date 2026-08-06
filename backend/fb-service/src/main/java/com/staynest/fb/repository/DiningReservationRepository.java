package com.staynest.fb.repository;

import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface DiningReservationRepository extends JpaRepository<DiningReservation, Integer> {
    List<DiningReservation> findByGuestId(Integer guestId);
    List<DiningReservation> findByDate(LocalDate date);
    List<DiningReservation> findByStatus(DiningResStatus status);

    /**
     * Detects a guest double-booking the same outlet for the same slot. The status
     * filter is what lets a guest rebook a slot they previously cancelled — only live
     * bookings count as a clash.
     */
    boolean existsByGuestIdAndRestaurantOutletIgnoreCaseAndDateAndTimeAndStatusIn(
            Integer guestId, String restaurantOutlet, LocalDate date, LocalTime time,
            Collection<DiningResStatus> statuses);
}