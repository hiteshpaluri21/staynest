package com.staynest.iam.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The user behind the current request, taken from the JWT.
 *
 * The same helper the other six services carry next to their AuditRecorder. iam-service
 * writes to audit_logs directly rather than through a Feign client, so it had no way to
 * name the acting user and recorded the affected account's id instead — which made the
 * userId column mean the actor on every row except the User ones.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The acting user's IAM id, or null when the request is unauthenticated, e.g. self-registration. */
    public static Integer id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getDetails() instanceof Integer id ? id : null;
    }

    /** The acting user's email, which is the JWT subject. */
    public static String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof String email ? email : null;
    }
}
