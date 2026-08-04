package com.staynest.reservation.client;

import com.staynest.reservation.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Best-effort fallback for {@link IamServiceClient}. Both endpoints are used for
 * non-critical enrichment (resolving a guest's real identity for an auto-created
 * profile, and fanning out staff notifications), so when iam-service is down we
 * return an empty response ({@code data == null}) and let the caller degrade to
 * its placeholder / no-op path. This is NOT used for hard validation.
 */
@Component
public class IamServiceClientFallback implements FallbackFactory<IamServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(IamServiceClientFallback.class);

    @Override
    public IamServiceClient create(Throwable cause) {
        return new IamServiceClient() {
            @Override
            public ApiResponse<Map<String, Object>> getUserByEmail(String email) {
                log.warn("iam-service unavailable, cannot resolve user by email {}: {}", email, cause.getMessage());
                return ApiResponse.error("iam-service unavailable");
            }

            @Override
            public ApiResponse<List<Map<String, Object>>> getUsersByRole(String role) {
                log.warn("iam-service unavailable, cannot resolve staff for role {}: {}", role, cause.getMessage());
                return ApiResponse.error("iam-service unavailable");
            }
        };
    }
}
