package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Conta toda tentativa igual, exista ou não a conta com o e-mail informado —
 * senão o próprio limite viraria um jeito de descobrir quem tem cadastro.
 */
@Component
public class PasswordResetRateLimiter {
    private static final String SCOPE = "PASSWORD_RESET";

    private final RateLimitStore store;
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public PasswordResetRateLimiter(
            RateLimitStore store,
            Clock clock,
            @Value("${app.rate-limit.password-reset.max-attempts}") int maxAttempts,
            @Value("${app.rate-limit.password-reset.window-seconds}") long windowSeconds
    ) {
        this.store = store;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void recordAttempt(String key) {
        Instant now = clock.instant();
        RateLimitStore.Window window = store.increment(SCOPE, key, now, windowSeconds);
        if (window.attempts() > maxAttempts) {
            throw new RateLimitExceededException("Limite de pedidos excedido. Tente novamente mais tarde.");
        }
    }
}
