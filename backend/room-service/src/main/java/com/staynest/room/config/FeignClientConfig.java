package com.staynest.room.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propagates the caller's Authorization (JWT) header to downstream Feign calls so
 * cross-service requests (room -> reservation-service) pass security.
 *
 * Without this, the availability lookup in RoomServiceImpl#getAvailableRooms called
 * GET /api/reservations with no credentials. That endpoint requires authentication, so the
 * call was rejected, the circuit-breaker fallback returned an empty payload, and every room
 * was reported as available no matter how many were already booked.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return (RequestTemplate template) -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                template.header("Authorization", authHeader);
            }
        };
    }
}
