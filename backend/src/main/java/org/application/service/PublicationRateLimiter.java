package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class PublicationRateLimiter {
    private static final String SCOPE = "PUBLICATION";

    private final RateLimitStore store;
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public PublicationRateLimiter(
            RateLimitStore store,
            Clock clock,
            @Value("${app.rate-limit.publication.max-attempts}") int maxAttempts,
            @Value("${app.rate-limit.publication.window-seconds}") long windowSeconds
    ) {
        this.store = store;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void recordAttempt(UUID authorId) {
        Instant now = clock.instant();
        RateLimitStore.Window window = store.increment(SCOPE, authorId.toString(), now, windowSeconds);
        if (window.attempts() > maxAttempts) {
            OffsetDateTime nextAvailableAt = window.startedAt().plusSeconds(windowSeconds).atOffset(ZoneOffset.UTC);
            throw new RateLimitExceededException(
                    "Limite de publicações excedido. Tente novamente mais tarde.", nextAvailableAt);
        }
    }
}
