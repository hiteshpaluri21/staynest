package com.staynest.fb.enums;

public enum OrderType {
    /**
     * No longer accepted for new orders — eating at an outlet is booked through dining
     * reservations instead. Retained only so historical orders still deserialise.
     */
    @Deprecated
    DINEIN,
    INROOMDINING,
    TAKEAWAY
}