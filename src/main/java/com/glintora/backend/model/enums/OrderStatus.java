package com.glintora.backend.model.enums;

/**
 * Lifecycle of a customer order.
 * Values must stay lowercase to match the CHECK constraint in the schema.
 */
public enum OrderStatus {
    pending,
    confirmed,
    processing,
    shipped,
    delivered,
    cancelled,
    returned
}
