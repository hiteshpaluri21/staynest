package com.staynest.room.repository;

import com.staynest.room.entity.Room;
import com.staynest.room.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByRoomType_RoomTypeId(Integer roomTypeId);
    List<Room> findByFloor(Integer floor);
    boolean existsByRoomNumber(String roomNumber);
}