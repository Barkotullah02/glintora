package com.glintora.backend.model.enums;

/**
 * Lifecycle of a stock reservation against a customer order item.
 * Values must stay lowercase to match the CHECK constraint in the schema.
 */
public enum ReservationStatus {
    active,
    fulfilled,
    released
}
