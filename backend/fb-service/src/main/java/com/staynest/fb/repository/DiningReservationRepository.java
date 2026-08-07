package com.staynest.fb.repository;

import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface DiningReservationRepository extends JpaRepository<DiningReservation, Integer> {
    List<DiningReservation> findByGuestId(Integer guestId);
    List<DiningReservation> findByDate(LocalDate date);
    List<DiningReservation> findByStatus(DiningResStatus status);

    /**
     * Live bookings at one outlet on one day, which the service overlap-checks in memory
     * against the requested sitting. The status filter is what lets a slot be rebooked after
     * a cancellation — only CONFIRMED and SEATED bookings hold the room.
     *
     * Overlap is not expressed as a derived query on purpose: end times may be null on older
     * rows, and a day's bookings for a single outlet are a handful of records.
     */
    List<DiningReservation> findByRestaurantOutletIgnoreCaseAndDateAndStatusIn(
            String restaurantOutlet, LocalDate date, Collection<DiningResStatus> statuses);
}