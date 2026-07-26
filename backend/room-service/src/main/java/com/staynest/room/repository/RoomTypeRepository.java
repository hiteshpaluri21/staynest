package com.staynest.room.repository;

import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RatePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {
    List<RoomType> findByStatus(RatePlanStatus status);
}