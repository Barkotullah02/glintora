package com.glintora.backend.model.enums;

/**
 * Types of inventory movements. Every stock change must post one of these.
 * Positive quantity = stock in, negative quantity = stock out.
 */
public enum MovementType {
    receipt,
    sale_dispatch,
    customer_return,
    supplier_return,
    damage_writeoff,
    adjustment_in,
    adjustment_out,
    transfer_in,
    transfer_out
}
