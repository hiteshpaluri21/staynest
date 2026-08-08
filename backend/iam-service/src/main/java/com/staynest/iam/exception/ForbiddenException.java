package com.staynest.iam.exception;

/**
 * The caller was identified but is not allowed through — maps to 403 while keeping
 * its own message, unlike Spring's AccessDeniedException which is answered with a
 * fixed one.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
