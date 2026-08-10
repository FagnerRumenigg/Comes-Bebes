package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

class LoginRateLimiterTest {
    @Test
    void shouldBlockAfterConfiguredFailuresAndAllowAfterReset() {
        LoginRateLimiter limiter = new LoginRateLimiter(
                Clock.fixed(Instant.parse("2026-08-08T15:00:00Z"), ZoneOffset.UTC), 2, 300);

        limiter.check("ip|email");
        limiter.registerFailure("ip|email");
        limiter.check("ip|email");
        limiter.registerFailure("ip|email");

        assertThatThrownBy(() -> limiter.check("ip|email"))
                .isInstanceOf(RateLimitExceededException.class);

        limiter.reset("ip|email");
        limiter.check("ip|email");
    }
}
