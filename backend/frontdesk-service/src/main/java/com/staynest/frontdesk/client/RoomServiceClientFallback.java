package com.staynest.frontdesk.client;

import com.staynest.frontdesk.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Best-effort fallback for {@link RoomServiceClient}. Updating a room's status on
 * check-in/check-out is a downstream side effect the caller already treats as
 * best-effort (it logs and continues). When room-service is down we return an
 * error response so the caller degrades cleanly instead of blocking on a timeout.
 */
@Component
public class RoomServiceClientFallback implements FallbackFactory<RoomServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceClientFallback.class);

    @Override
    public RoomServiceClient create(Throwable cause) {
        return (id, status) -> {
            log.warn("room-service unavailable, could not set room {} status to {}: {}", id, status, cause.getMessage());
            return ApiResponse.error("room-service unavailable");
        };
    }
}
