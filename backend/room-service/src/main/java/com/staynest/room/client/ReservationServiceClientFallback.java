package com.staynest.room.client;

import com.staynest.room.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Best-effort fallback for {@link ReservationServiceClient}. Availability
 * filtering enriches the room list with live reservation data; when
 * reservation-service is down we return an empty response ({@code data == null})
 * and the caller falls back to listing all AVAILABLE rooms.
 */
@Component
public class ReservationServiceClientFallback implements FallbackFactory<ReservationServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceClientFallback.class);

    @Override
    public ReservationServiceClient create(Throwable cause) {
        return (String status) -> {
            log.warn("reservation-service unavailable, skipping availability filter: {}", cause.getMessage());
            return ApiResponse.error("reservation-service unavailable");
        };
    }
}
