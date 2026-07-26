package com.staynest.fb.repository;

import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiningReservationRepository extends JpaRepository<DiningReservation, Integer> {
    List<DiningReservation> findByGuestId(Integer guestId);
    List<DiningReservation> findByDate(LocalDate date);
    List<DiningReservation> findByStatus(DiningResStatus status);
}