package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    private static final Instant NOW = Instant.parse("2026-08-08T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private RateLimitStore store;

    @Test
    void shouldThrowWhenStoreReportsAttemptsAtOrAboveMax() {
        LoginRateLimiter limiter = new LoginRateLimiter(store, CLOCK, 2, 300);
        when(store.currentWindow(eq("LOGIN"), eq("ip|email"), eq(NOW), eq(300L)))
                .thenReturn(new RateLimitStore.Window(NOW, 2));

        assertThatThrownBy(() -> limiter.check("ip|email"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void shouldNotThrowWhenStoreReportsAttemptsBelowMax() {
        LoginRateLimiter limiter = new LoginRateLimiter(store, CLOCK, 2, 300);
        when(store.currentWindow(eq("LOGIN"), eq("ip|email"), eq(NOW), eq(300L)))
                .thenReturn(new RateLimitStore.Window(NOW, 1));

        assertThatCode(() -> limiter.check("ip|email")).doesNotThrowAnyException();
    }

    @Test
    void shouldDelegateFailureToStoreIncrement() {
        LoginRateLimiter limiter = new LoginRateLimiter(store, CLOCK, 2, 300);

        limiter.registerFailure("ip|email");

        verify(store).increment("LOGIN", "ip|email", NOW, 300L);
    }

    @Test
    void shouldDelegateResetToStoreClear() {
        LoginRateLimiter limiter = new LoginRateLimiter(store, CLOCK, 2, 300);

        limiter.reset("ip|email");

        verify(store).clear("LOGIN", "ip|email");
    }
}
