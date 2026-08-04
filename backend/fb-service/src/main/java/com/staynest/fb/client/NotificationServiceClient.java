package com.staynest.fb.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Fire-and-forget client for the notification-service. Body keys: {@code userId} (Integer),
 * {@code message} (String), {@code category} (RESERVATION|FRONTDESK|HOUSEKEEPING|FB|REVENUE).
 */
@FeignClient(name = "NOTIFICATION-SERVICE", fallbackFactory = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications")
    Object create(@RequestBody Map<String, Object> body);
}
