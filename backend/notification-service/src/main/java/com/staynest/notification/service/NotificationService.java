package com.staynest.notification.service;

import com.staynest.notification.dto.NotificationRequest;
import com.staynest.notification.dto.NotificationResponse;
import com.staynest.notification.dto.UnreadCountResponse;

import java.util.List;

public interface NotificationService {
    NotificationResponse sendNotification(NotificationRequest request);
    NotificationResponse markAsRead(Integer notificationId);
    int markAllAsRead(Integer userId);
    UnreadCountResponse getUnreadCount(Integer userId);
    NotificationResponse getNotificationById(Integer id);
    List<NotificationResponse> getNotificationsByUser(Integer userId);
    List<NotificationResponse> getUnreadNotificationsByUser(Integer userId);
}