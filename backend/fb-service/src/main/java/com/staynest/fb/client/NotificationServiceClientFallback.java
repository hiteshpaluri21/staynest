package com.staynest.fb.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Best-effort fallback for {@link NotificationServiceClient}. Notifications are
 * fire-and-forget, so when notification-service is down we log and drop the
 * message rather than failing the primary action.
 */
@Component
public class NotificationServiceClientFallback implements FallbackFactory<NotificationServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClientFallback.class);

    @Override
    public NotificationServiceClient create(Throwable cause) {
        return (Map<String, Object> body) -> {
            log.warn("notification-service unavailable, dropping notification {}: {}", body, cause.getMessage());
            return null;
        };
    }
}
