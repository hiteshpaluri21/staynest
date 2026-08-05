package com.staynest.room.exception;

/**
 * Thrown when a downstream service this request depends on cannot be reached, so the answer
 * would otherwise be silently wrong. Maps to 503.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
