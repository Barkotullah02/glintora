package com.glintora.backend.model.enums;

/**
 * Status of a payment record. Values match the schema CHECK constraint.
 */
public enum PaymentStatus {
    pending,
    paid,
    failed,
    refunded
}
