package com.staynest.notification.serviceimpl;

import com.staynest.notification.audit.AuditRecorder;
import com.staynest.notification.dto.NotificationRequest;
import com.staynest.notification.dto.NotificationResponse;
import com.staynest.notification.dto.UnreadCountResponse;
import com.staynest.notification.entity.Notification;
import com.staynest.notification.enums.NotificationCategory;
import com.staynest.notification.enums.NotificationStatus;
import com.staynest.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The unread bell: a new notification arrives unread, reading one or all of them clears it, and
 * the badge count only ever counts what is still unread for that one user.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    private static final int USER_ID = 7;
    private static final int NOTIFICATION_ID = 31;

    @Mock private AuditRecorder auditRecorder;
    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationServiceImpl service;

    private static NotificationRequest request() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(USER_ID);
        req.setMessage("Your reservation #55 is confirmed.");
        req.setCategory(NotificationCategory.RESERVATION);
        return req;
    }

    private static Notification notification(int id, NotificationStatus status) {
        return Notification.builder()
                .notificationId(id)
                .userId(USER_ID)
                .message("Your reservation #55 is confirmed.")
                .category(NotificationCategory.RESERVATION)
                .status(status)
                .build();
    }

    @Test
    void sendNotification_unread() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setNotificationId(NOTIFICATION_ID);
            return n;
        });

        NotificationResponse sent = service.sendNotification(request());

        assertThat(sent.getNotificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        // Newly delivered, so it must show on the bell until the user opens it.
        assertThat(sent.getStatus()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    void markAsRead_changes() {
        when(notificationRepository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification(NOTIFICATION_ID, NotificationStatus.UNREAD)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse read = service.markAsRead(NOTIFICATION_ID);

        assertThat(read.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(auditRecorder).record("MARK_READ", "NOTIFICATION", NOTIFICATION_ID);
    }

    @Test
    void getUnreadCount_correct() {
        when(notificationRepository.countByUserIdAndStatus(USER_ID, NotificationStatus.UNREAD))
                .thenReturn(3L);

        UnreadCountResponse count = service.getUnreadCount(USER_ID);

        assertThat(count.getUnreadCount()).isEqualTo(3);
    }

    /** "Mark all read" must flip every unread row for that user, and report how many it touched. */
    @Test
    void markAllAsRead_updatesAll() {
        List<Notification> unread = List.of(
                notification(31, NotificationStatus.UNREAD),
                notification(32, NotificationStatus.UNREAD),
                notification(33, NotificationStatus.UNREAD));
        when(notificationRepository.findByUserIdAndStatus(USER_ID, NotificationStatus.UNREAD))
                .thenReturn(unread);

        int cleared = service.markAllAsRead(USER_ID);

        assertThat(cleared).isEqualTo(3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .allSatisfy(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ));
        verify(auditRecorder).record("MARK_ALL_READ", "NOTIFICATION", null);
    }
}
