package com.staynest.room.repository;

import com.staynest.room.entity.RatePlan;
import com.staynest.room.enums.RatePlanName;
import com.staynest.room.enums.RatePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, Integer> {

    /**
     * Plans of the same name, for the same room type, whose validity window touches
     * [validFrom, validTo]. Two ranges overlap when each starts on or before the other
     * ends, which is the condition below.
     *
     * Only ACTIVE plans are considered — a deactivated plan is history and should not
     * stand in the way of a new one.
     */
    @Query("""
           select rp from RatePlan rp
           where rp.roomType.roomTypeId = :roomTypeId
             and rp.name = :name
             and rp.status = com.staynest.room.enums.RatePlanStatus.ACTIVE
             and rp.validFrom <= :validTo
             and rp.validTo >= :validFrom
           """)
    List<RatePlan> findActiveOverlapping(@Param("roomTypeId") Integer roomTypeId,
                                        @Param("name") RatePlanName name,
                                        @Param("validFrom") LocalDate validFrom,
                                        @Param("validTo") LocalDate validTo);

    List<RatePlan> findByRoomType_RoomTypeId(Integer roomTypeId);
    List<RatePlan> findByStatus(RatePlanStatus status);
    List<RatePlan> findByRoomType_RoomTypeIdAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            Integer roomTypeId, RatePlanStatus status, LocalDate checkIn, LocalDate checkOut);
}