package com.staynest.room.repository;

import com.staynest.room.entity.RatePlan;
import com.staynest.room.enums.RatePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, Integer> {
    List<RatePlan> findByRoomType_RoomTypeId(Integer roomTypeId);
    List<RatePlan> findByStatus(RatePlanStatus status);
    List<RatePlan> findByRoomType_RoomTypeIdAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            Integer roomTypeId, RatePlanStatus status, LocalDate checkIn, LocalDate checkOut);
}