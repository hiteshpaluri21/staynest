package com.staynest.iam.validation;

/**
 * Shared regexes and messages for request validation, so the same rule is not re-spelled
 * (and quietly diverged) across DTOs.
 */
public final class ValidationPatterns {

    /**
     * Exactly 10 national digits, optionally preceded by a "+" country code of 1-3 digits with
     * an optional space or dash separator. Accepts "9876543210", "+919876543210", "+91 9876543210".
     */
    public static final String PHONE = "^(?:\\+\\d{1,3}[ -]?)?\\d{10}$";

    public static final String PHONE_MESSAGE =
            "must be exactly 10 digits, optionally prefixed with a + country code (e.g. +91 9876543210)";

    private ValidationPatterns() {
    }
}
