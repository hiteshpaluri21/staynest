package com.staynest.housekeeping.repository;

import com.staynest.housekeeping.entity.MaintenanceRequest;
import com.staynest.housekeeping.enums.MaintenancePriority;
import com.staynest.housekeeping.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Integer> {
    List<MaintenanceRequest> findByRoomId(Integer roomId);
    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);
    List<MaintenanceRequest> findByPriority(MaintenancePriority priority);
    List<MaintenanceRequest> findByReportedBy(Integer reportedBy);
}