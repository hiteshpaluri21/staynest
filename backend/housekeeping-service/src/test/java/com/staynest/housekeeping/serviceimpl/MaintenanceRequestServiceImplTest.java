package com.staynest.housekeeping.serviceimpl;

import com.staynest.housekeeping.audit.AuditRecorder;
import com.staynest.housekeeping.client.IamServiceClient;
import com.staynest.housekeeping.client.NotificationServiceClient;
import com.staynest.housekeeping.dto.MaintenanceRequestDto;
import com.staynest.housekeeping.dto.MaintenanceResponse;
import com.staynest.housekeeping.entity.MaintenanceRequest;
import com.staynest.housekeeping.enums.MaintenancePriority;
import com.staynest.housekeeping.enums.MaintenanceStatus;
import com.staynest.housekeeping.repository.MaintenanceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A reported fault opens on the board, and resolving it records the day it was fixed — the
 * resolvedDate is what "how long was this room broken for" is answered from.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintenanceRequestServiceImplTest {

    private static final int REQUEST_ID = 5;
    private static final int ROOM_ID = 101;
    private static final int REPORTER_ID = 12;

    @Mock private AuditRecorder auditRecorder;
    @Mock private MaintenanceRequestRepository maintenanceRepository;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private IamServiceClient iamServiceClient;
    @InjectMocks private MaintenanceRequestServiceImpl service;

    private static MaintenanceRequestDto request() {
        MaintenanceRequestDto dto = new MaintenanceRequestDto();
        dto.setRoomId(ROOM_ID);
        dto.setReportedBy(REPORTER_ID);
        dto.setIssueDescription("Air conditioning not cooling");
        dto.setPriority(MaintenancePriority.HIGH);
        return dto;
    }

    private static MaintenanceRequest existing(MaintenanceStatus status, LocalDate resolvedDate) {
        return MaintenanceRequest.builder()
                .requestId(REQUEST_ID)
                .roomId(ROOM_ID)
                .reportedBy(REPORTER_ID)
                .issueDescription("Air conditioning not cooling")
                .priority(MaintenancePriority.HIGH)
                .status(status)
                .resolvedDate(resolvedDate)
                .build();
    }

    @Test
    void reportIssue_savedOpen() {
        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest mr = inv.getArgument(0);
            mr.setRequestId(REQUEST_ID);
            return mr;
        });

        MaintenanceResponse reported = service.reportIssue(request());

        assertThat(reported.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(reported.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(reported.getPriority()).isEqualTo(MaintenancePriority.HIGH);
        // Open, dated today, and not yet resolved.
        assertThat(reported.getStatus()).isEqualTo(MaintenanceStatus.OPEN);
        assertThat(reported.getRaisedDate()).isEqualTo(LocalDate.now());
        assertThat(reported.getResolvedDate()).isNull();
        verify(auditRecorder).record("CREATE", "MAINTENANCEREQUEST", REQUEST_ID);
    }

    @Test
    void resolveRequest_setsResolvedAndDate() {
        when(maintenanceRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(existing(MaintenanceStatus.INPROGRESS, null)));
        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceResponse resolved = service.resolveRequest(REQUEST_ID);

        assertThat(resolved.getStatus()).isEqualTo(MaintenanceStatus.RESOLVED);
        assertThat(resolved.getResolvedDate()).isEqualTo(LocalDate.now());
        verify(auditRecorder).record("RESOLVE", "MAINTENANCEREQUEST", REQUEST_ID);
    }
}
