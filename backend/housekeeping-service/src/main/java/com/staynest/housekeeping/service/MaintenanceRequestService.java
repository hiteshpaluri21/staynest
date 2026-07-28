package com.staynest.housekeeping.service;

import com.staynest.housekeeping.dto.MaintenanceRequestDto;
import com.staynest.housekeeping.dto.MaintenanceResponse;
import com.staynest.housekeeping.enums.MaintenanceStatus;

import java.util.List;

public interface MaintenanceRequestService {
    MaintenanceResponse reportIssue(MaintenanceRequestDto request);
    MaintenanceResponse updateStatus(Integer requestId, MaintenanceStatus status);
    MaintenanceResponse resolveRequest(Integer requestId);
    MaintenanceResponse getRequestById(Integer requestId);
    List<MaintenanceResponse> getAllRequests();
    List<MaintenanceResponse> getRequestsByStatus(MaintenanceStatus status);
    List<MaintenanceResponse> getRequestsByRoomId(Integer roomId);
    List<MaintenanceResponse> getRequestsByReportedBy(Integer reportedBy);
}