package com.glintora.backend.service;

/** Thrown when a requested quantity cannot be reserved from stock. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
