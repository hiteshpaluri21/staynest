package com.staynest.frontdesk.repository;

import com.staynest.frontdesk.entity.StayRecord;
import com.staynest.frontdesk.enums.StayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StayRecordRepository extends JpaRepository<StayRecord, Integer> {
    List<StayRecord> findByGuestId(Integer guestId);
    List<StayRecord> findByStatus(StayStatus status);
    Optional<StayRecord> findByReservationId(Integer reservationId);
    Optional<StayRecord> findByAssignedRoomIdAndStatus(Integer assignedRoomId, StayStatus status);
}