package com.staynest.housekeeping.repository;

import com.staynest.housekeeping.entity.HousekeepingTask;
import com.staynest.housekeeping.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Integer> {
    List<HousekeepingTask> findByRoomId(Integer roomId);
    List<HousekeepingTask> findByStatus(TaskStatus status);
    List<HousekeepingTask> findByAssignedToId(Integer assignedToId);
}