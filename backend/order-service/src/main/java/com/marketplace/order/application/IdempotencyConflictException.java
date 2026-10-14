package com.marketplace.order.application;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("The idempotency key was already used for a different order payload");
    }
}
