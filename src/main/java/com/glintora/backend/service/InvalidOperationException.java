package com.glintora.backend.service;

/** Business-rule violation that is not caused by stock levels. */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
