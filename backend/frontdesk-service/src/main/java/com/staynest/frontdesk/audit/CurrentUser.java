package com.staynest.frontdesk.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The user behind the current request, taken from the JWT.
 *
 * JwtFilter puts the userId claim on the Authentication's details, so the service layer can
 * record who acted without threading an id through every method signature.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The acting user's IAM id, or null when the request is unauthenticated or the token is old. */
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
