package com.staynest.housekeeping.client;

import com.staynest.housekeeping.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Best-effort fallback for {@link IamServiceClient}. The staff-role lookup only
 * drives notification fan-out, so when iam-service is down we return an empty
 * response ({@code data == null}) and the caller skips notifying staff. This is
 * NOT used for hard validation.
 */
@Component
public class IamServiceClientFallback implements FallbackFactory<IamServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(IamServiceClientFallback.class);

    @Override
    public IamServiceClient create(Throwable cause) {
        return new IamServiceClient() {
            @Override
            public ApiResponse<List<Map<String, Object>>> getUsersByRole(String role) {
                log.warn("iam-service unavailable, cannot resolve staff for role {}: {}", role, cause.getMessage());
                return ApiResponse.error("iam-service unavailable");
            }
        };
    }
}
