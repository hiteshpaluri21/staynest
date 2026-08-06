package com.staynest.reservation.repository;

import com.staynest.reservation.entity.Reservation;
import com.staynest.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    List<Reservation> findByGuest_GuestId(Integer guestId);
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findByCheckInDate(LocalDate checkInDate);
    List<Reservation> findByCheckInDateBetween(LocalDate start, LocalDate end);
    List<Reservation> findByCheckInDateGreaterThanEqual(LocalDate date);

    // Enum literals inside JPQL stay fully qualified — the unqualified form depends on
    // Hibernate version support, and this must not be version-sensitive.
    @Query("SELECT r FROM Reservation r WHERE r.roomTypeId = :roomTypeId AND r.status IN (com.staynest.reservation.enums.ReservationStatus.CONFIRMED, com.staynest.reservation.enums.ReservationStatus.CHECKEDIN) AND r.checkInDate < :checkOutDate AND r.checkOutDate > :checkInDate")
    List<Reservation> findOverlappingReservations(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);
}