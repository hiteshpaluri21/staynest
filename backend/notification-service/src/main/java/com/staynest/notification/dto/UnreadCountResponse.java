package com.staynest.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnreadCountResponse {
    private Integer unreadCount;
}