package com.staynest.notification.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propagates the caller's Authorization (JWT) header to downstream Feign calls so
 * cross-service requests (notification -> iam-service) pass security.
 *
 * The other five services that call out already had this; notification-service gained an
 * AuditServiceClient without it. POST /api/audit-logs requires authentication, so every
 * MARK_READ and MARK_ALL_READ write was rejected with a 403 and then swallowed by
 * AuditRecorder's catch — those two actions never reached the trail at all.
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
