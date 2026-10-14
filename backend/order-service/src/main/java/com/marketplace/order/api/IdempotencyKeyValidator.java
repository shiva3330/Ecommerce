package com.marketplace.order.api;

public final class IdempotencyKeyValidator {

    public static final int MAX_LENGTH = 255;

    private IdempotencyKeyValidator() {
    }

    public static boolean isValid(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_LENGTH) {
            return false;
        }
        return key.codePoints().allMatch(codePoint ->
                codePoint >= 0x20
                        && codePoint != 0x7f
                        && !Character.isISOControl(codePoint)
        );
    }
}
