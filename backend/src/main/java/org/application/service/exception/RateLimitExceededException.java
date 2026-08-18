package org.application.service.exception;

import java.time.OffsetDateTime;

public class RateLimitExceededException extends RuntimeException {
    private final OffsetDateTime nextAvailableAt;

    public RateLimitExceededException(String message) {
        this(message, null);
    }

    public RateLimitExceededException(String message, OffsetDateTime nextAvailableAt) {
        super(message);
        this.nextAvailableAt = nextAvailableAt;
    }

    public OffsetDateTime nextAvailableAt() {
        return nextAvailableAt;
    }
}
