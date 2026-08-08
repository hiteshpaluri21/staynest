package com.staynest.iam.exception;

/** Credentials were absent or wrong — maps to 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
