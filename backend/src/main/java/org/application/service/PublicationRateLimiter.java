package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PublicationRateLimiter {
    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public PublicationRateLimiter(
            Clock clock,
            @Value("${app.rate-limit.publication.max-attempts}") int maxAttempts,
            @Value("${app.rate-limit.publication.window-seconds}") long windowSeconds
    ) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void recordAttempt(UUID authorId) {
        Instant now = clock.instant();
        AtomicBoolean exceeded = new AtomicBoolean(false);
        Window window = windows.compute(authorId, (ignored, current) -> {
            Window base = current == null || expired(current, now) ? new Window(now, 0) : current;
            if (base.attempts() >= maxAttempts) {
                exceeded.set(true);
                return base;
            }
            return new Window(base.startedAt(), base.attempts() + 1);
        });
        if (exceeded.get()) {
            OffsetDateTime nextAvailableAt = window.startedAt().plusSeconds(windowSeconds).atOffset(ZoneOffset.UTC);
            throw new RateLimitExceededException(
                    "Limite de publicações excedido. Tente novamente mais tarde.", nextAvailableAt);
        }
    }

    private boolean expired(Window window, Instant now) {
        return now.isAfter(window.startedAt().plusSeconds(windowSeconds));
    }

    private record Window(Instant startedAt, int attempts) {}
}
