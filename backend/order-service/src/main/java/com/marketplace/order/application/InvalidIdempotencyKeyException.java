package com.marketplace.order.application;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must be printable, non-blank, and at most 255 characters");
    }
}
