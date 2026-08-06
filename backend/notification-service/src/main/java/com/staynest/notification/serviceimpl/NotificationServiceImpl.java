package com.staynest.notification.serviceimpl;

import com.staynest.notification.audit.AuditRecorder;
import com.staynest.notification.dto.NotificationRequest;
import com.staynest.notification.dto.NotificationResponse;
import com.staynest.notification.dto.UnreadCountResponse;
import com.staynest.notification.entity.Notification;
import com.staynest.notification.enums.NotificationStatus;
import com.staynest.notification.exception.ResourceNotFoundException;
import com.staynest.notification.repository.NotificationRepository;
import com.staynest.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "NOTIFICATION";

    private final AuditRecorder auditRecorder;
    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .message(request.getMessage())
                .category(request.getCategory())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent to user {}: {}", request.getUserId(), saved.getNotificationId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        notification.setStatus(NotificationStatus.READ);
        Notification updated = notificationRepository.save(notification);
        log.info("Notification {} marked as read", notificationId);
        auditRecorder.record("MARK_READ", ENTITY, notificationId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public int markAllAsRead(Integer userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndStatus(userId, NotificationStatus.UNREAD);
        
        unreadNotifications.forEach(n -> n.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unreadNotifications);
        
        int count = unreadNotifications.size();
        log.info("Marked {} notifications as read for user {}", count, userId);
        auditRecorder.record("MARK_ALL_READ", ENTITY, null);
        return count;
    }

    @Override
    public UnreadCountResponse getUnreadCount(Integer userId) {
        long count = notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
        return UnreadCountResponse.builder()
                .unreadCount((int) count)
                .build();
    }

    @Override
    public NotificationResponse getNotificationById(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        return mapToResponse(notification);
    }

    @Override
    public List<NotificationResponse> getNotificationsByUser(Integer userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotificationsByUser(Integer userId) {
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.UNREAD).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .message(n.getMessage())
                .category(n.getCategory())
                .status(n.getStatus())
                .createdDate(n.getCreatedDate())
                .build();
    }
}