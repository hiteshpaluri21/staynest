package com.staynest.housekeeping.serviceimpl;

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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRepository;

    @Override
    public MaintenanceResponse reportIssue(MaintenanceRequestDto request) {
        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .roomId(request.getRoomId())
                .reportedBy(request.getReportedBy())
                .issueDescription(request.getIssueDescription())
                .priority(request.getPriority())
                .build();

        MaintenanceRequest saved = maintenanceRepository.save(maintenance);
        log.info("Maintenance request created: {}", saved.getRequestId());
        return mapToResponse(saved);
    }

    @Override
    public MaintenanceResponse updateStatus(Integer requestId, MaintenanceStatus status) {
        MaintenanceRequest request = maintenanceRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));
        
        request.setStatus(status);
        MaintenanceRequest updated = maintenanceRepository.save(request);
        log.info("Maintenance request {} status updated to {}", requestId, status);
        return mapToResponse(updated);
    }

    @Override
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