package com.staynest.fb.client;

import feign.FeignException;

/** Helpers for interpreting failures from Feign cross-service calls. */
public final class FeignErrors {

    private FeignErrors() {}

    /**
     * Whether the failure is really a downstream {@code 404 Not Found}, i.e. "no such record"
     * rather than an outage. With the Feign circuit breaker enabled the
     * {@link FeignException.NotFound} arrives wrapped (e.g. in {@code NoFallbackAvailableException}),
     * so the cause chain has to be walked — otherwise a plain "not found" surfaces to callers as an
     * opaque "No fallback available" message.
     */
    public static boolean isNotFound(Throwable t) {
        for (Throwable c = t; c != null && c.getCause() != c; c = c.getCause()) {
            if (c instanceof FeignException.NotFound) {
                return true;
            }
        }
        return false;
    }
}
