package com.staynest.notification.dto;

import com.staynest.notification.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {
    @NotNull
    private Integer userId;
    @NotBlank
    private String message;
    @NotNull
    private NotificationCategory category;
}