package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class LoginRateLimiter {
    private static final String SCOPE = "LOGIN";

    private final RateLimitStore store;
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public LoginRateLimiter(
            RateLimitStore store,
            Clock clock,
            @Value("${app.rate-limit.login.max-attempts}") int maxAttempts,
            @Value("${app.rate-limit.login.window-seconds}") long windowSeconds
    ) {
        this.store = store;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void check(String key) {
        Instant now = clock.instant();
        RateLimitStore.Window window = store.currentWindow(SCOPE, key, now, windowSeconds);
        if (window.attempts() >= maxAttempts) {
            throw new RateLimitExceededException("Limite de tentativas excedido. Tente novamente mais tarde.");
        }
    }

    public void registerFailure(String key) {
        store.increment(SCOPE, key, clock.instant(), windowSeconds);
    }

    public void reset(String key) {
        store.clear(SCOPE, key);
    }
}
