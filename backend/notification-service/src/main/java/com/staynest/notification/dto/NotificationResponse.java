package com.staynest.notification.dto;

import com.staynest.notification.enums.NotificationCategory;
import com.staynest.notification.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Integer notificationId;
    private Integer userId;
    private String message;
    private NotificationCategory category;
    private NotificationStatus status;
    private LocalDateTime createdDate;
}