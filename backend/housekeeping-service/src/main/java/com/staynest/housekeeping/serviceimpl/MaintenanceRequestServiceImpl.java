package com.staynest.housekeeping.serviceimpl;

import com.staynest.housekeeping.audit.AuditRecorder;
import com.staynest.housekeeping.dto.MaintenanceRequestDto;
import com.staynest.housekeeping.dto.MaintenanceResponse;
import com.staynest.housekeeping.entity.MaintenanceRequest;
import com.staynest.housekeeping.enums.MaintenanceStatus;
import com.staynest.housekeeping.exception.BadRequestException;
import com.staynest.housekeeping.exception.ResourceNotFoundException;
import com.staynest.housekeeping.repository.MaintenanceRequestRepository;
import com.staynest.housekeeping.service.MaintenanceRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.staynest.housekeeping.client.IamServiceClient;
import com.staynest.housekeeping.client.NotificationServiceClient;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "MAINTENANCEREQUEST";

    private final AuditRecorder auditRecorder;

    private final MaintenanceRequestRepository maintenanceRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final IamServiceClient iamServiceClient;

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(Map.of(
                    "userId", userId, "category", "HOUSEKEEPING", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send HOUSEKEEPING notification to user {}: {}", userId, e.getMessage());
        }
    }

    /** Fan out a notification to every active HOUSEKEEPING staff member. Best-effort. */
    private void notifyHousekeepingStaff(String message) {
        try {
            var resp = iamServiceClient.getUsersByRole("HOUSEKEEPING");
            var staff = resp != null ? resp.getData() : null;
            if (staff == null) return;
            for (var u : staff) {
                Object id = u.get("userId");
                if (id instanceof Number n) {
                    notify(n.intValue(), message);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve HOUSEKEEPING staff for notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public MaintenanceResponse reportIssue(MaintenanceRequestDto request) {
        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .roomId(request.getRoomId())
                .reportedBy(request.getReportedBy())
                .issueDescription(request.getIssueDescription())
                .priority(request.getPriority())
                .build();

        MaintenanceRequest saved = maintenanceRepository.save(maintenance);
        log.info("Maintenance request created: {}", saved.getRequestId());
        auditRecorder.record("CREATE", ENTITY, saved.getRequestId());
        // Acknowledge the reporter, and alert the housekeeping team of the new request.
        notify(saved.getReportedBy(), "Your maintenance request #" + saved.getRequestId()
                + " for room #" + saved.getRoomId() + " has been logged.");
        notifyHousekeepingStaff("New " + saved.getPriority() + " maintenance request #" + saved.getRequestId()
                + " raised for room #" + saved.getRoomId() + ".");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateStatus(Integer requestId, MaintenanceStatus status) {
        MaintenanceRequest request = maintenanceRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));
        
        request.setStatus(status);
        // Keep resolvedDate consistent with the status.
        if (status == MaintenanceStatus.RESOLVED) {
            if (request.getResolvedDate() == null) {
                request.setResolvedDate(LocalDate.now());
            }
        } else {
            request.setResolvedDate(null);
        }
        MaintenanceRequest updated = maintenanceRepository.save(request);
        log.info("Maintenance request {} status updated to {}", requestId, status);
        auditRecorder.record("UPDATE_STATUS", ENTITY, requestId);
        notify(updated.getReportedBy(), "Your maintenance request #" + updated.getRequestId()
                + " is now " + status + ".");
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public MaintenanceResponse resolveRequest(Integer requestId) {
        MaintenanceRequest request = maintenanceRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));
        
        if (request.getStatus() == MaintenanceStatus.RESOLVED) {
            throw new BadRequestException("Request is already resolved");
        }
        
        request.setStatus(MaintenanceStatus.RESOLVED);
        request.setResolvedDate(LocalDate.now());
        MaintenanceRequest updated = maintenanceRepository.save(request);
        log.info("Maintenance request {} resolved", requestId);
        auditRecorder.record("RESOLVE", ENTITY, requestId);
        return mapToResponse(updated);
    }

    @Override
    public MaintenanceResponse getRequestById(Integer requestId) {
        MaintenanceRequest request = maintenanceRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));
        return mapToResponse(request);
    }

    @Override
    public List<MaintenanceResponse> getAllRequests() {
        return maintenanceRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MaintenanceResponse> getRequestsByStatus(MaintenanceStatus status) {
        return maintenanceRepository.findByStatus(status).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MaintenanceResponse> getRequestsByRoomId(Integer roomId) {
        return maintenanceRepository.findByRoomId(roomId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MaintenanceResponse> getRequestsByReportedBy(Integer reportedBy) {
        return maintenanceRepository.findByReportedBy(reportedBy).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    private MaintenanceResponse mapToResponse(MaintenanceRequest mr) {
        return MaintenanceResponse.builder()
                .requestId(mr.getRequestId())
                .roomId(mr.getRoomId())
                .reportedBy(mr.getReportedBy())
                .issueDescription(mr.getIssueDescription())
                .priority(mr.getPriority())
                .raisedDate(mr.getRaisedDate())
                .resolvedDate(mr.getResolvedDate())
                .status(mr.getStatus())
                .build();
    }
}