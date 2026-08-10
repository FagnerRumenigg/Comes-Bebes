package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public LoginRateLimiter(
            Clock clock,
            @Value("${app.rate-limit.login.max-attempts}") int maxAttempts,
            @Value("${app.rate-limit.login.window-seconds}") long windowSeconds
    ) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void check(String key) {
        Instant now = clock.instant();
        Window window = windows.compute(key, (ignored, current) -> current == null || expired(current, now)
                ? new Window(now, 0)
                : current);
        if (window.attempts() >= maxAttempts) {
            throw new RateLimitExceededException("Limite de tentativas excedido. Tente novamente mais tarde.");
        }
    }

    public void registerFailure(String key) {
        Instant now = clock.instant();
        windows.compute(key, (ignored, current) -> current == null || expired(current, now)
                ? new Window(now, 1)
                : new Window(current.startedAt(), current.attempts() + 1));
    }

    public void reset(String key) {
        windows.remove(key);
    }

    private boolean expired(Window window, Instant now) {
        return now.isAfter(window.startedAt().plusSeconds(windowSeconds));
    }

    private record Window(Instant startedAt, int attempts) {}
}
