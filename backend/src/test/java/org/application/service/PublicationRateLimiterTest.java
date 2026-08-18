package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicationRateLimiterTest {
    @Test
    void shouldBlockAfterConfiguredAttemptsAndExposeNextAvailableAt() {
        UUID authorId = UUID.randomUUID();
        Instant start = Instant.parse("2026-08-08T15:00:00Z");
        PublicationRateLimiter limiter = new PublicationRateLimiter(
                Clock.fixed(start, ZoneOffset.UTC), 2, 600);

        limiter.recordAttempt(authorId);
        limiter.recordAttempt(authorId);

        assertThatThrownBy(() -> limiter.recordAttempt(authorId))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(exception -> assertThat(((RateLimitExceededException) exception).nextAvailableAt())
                        .isEqualTo(start.plusSeconds(600).atOffset(ZoneOffset.UTC)));
    }

    @Test
    void shouldAllowAgainAfterWindowExpires() {
        UUID authorId = UUID.randomUUID();
        Instant start = Instant.parse("2026-08-08T15:00:00Z");
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(start, start.plusSeconds(601));
        PublicationRateLimiter limiter = new PublicationRateLimiter(clock, 1, 600);

        limiter.recordAttempt(authorId);
        assertThatCode(() -> limiter.recordAttempt(authorId)).doesNotThrowAnyException();
    }

    @Test
    void shouldTrackAuthorsIndependently() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PublicationRateLimiter limiter = new PublicationRateLimiter(
                Clock.fixed(Instant.parse("2026-08-08T15:00:00Z"), ZoneOffset.UTC), 1, 600);

        limiter.recordAttempt(first);
        assertThatCode(() -> limiter.recordAttempt(second)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.recordAttempt(first)).isInstanceOf(RateLimitExceededException.class);
    }
}
