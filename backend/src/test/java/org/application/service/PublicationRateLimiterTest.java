package org.application.service;

import org.application.service.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicationRateLimiterTest {

    @Mock
    private RateLimitStore store;

    @Test
    void shouldBlockWhenStoreReportsAttemptsAboveMaxAndExposeNextAvailableAt() {
        UUID authorId = UUID.randomUUID();
        Instant start = Instant.parse("2026-08-08T15:00:00Z");
        PublicationRateLimiter limiter = new PublicationRateLimiter(store, Clock.fixed(start, ZoneOffset.UTC), 2, 600);
        when(store.increment("PUBLICATION", authorId.toString(), start, 600L))
                .thenReturn(new RateLimitStore.Window(start, 3));

        assertThatThrownBy(() -> limiter.recordAttempt(authorId))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(exception -> assertThat(((RateLimitExceededException) exception).nextAvailableAt())
                        .isEqualTo(start.plusSeconds(600).atOffset(ZoneOffset.UTC)));
    }

    @Test
    void shouldNotThrowWhenStoreReportsAttemptsAtOrBelowMax() {
        UUID authorId = UUID.randomUUID();
        Instant start = Instant.parse("2026-08-08T15:00:00Z");
        PublicationRateLimiter limiter = new PublicationRateLimiter(store, Clock.fixed(start, ZoneOffset.UTC), 2, 600);
        when(store.increment("PUBLICATION", authorId.toString(), start, 600L))
                .thenReturn(new RateLimitStore.Window(start, 2));

        assertThatCode(() -> limiter.recordAttempt(authorId)).doesNotThrowAnyException();
    }

    @Test
    void shouldUseAuthorIdAsStoreKey() {
        UUID authorId = UUID.randomUUID();
        Instant start = Instant.parse("2026-08-08T15:00:00Z");
        PublicationRateLimiter limiter = new PublicationRateLimiter(store, Clock.fixed(start, ZoneOffset.UTC), 5, 600);
        when(store.increment("PUBLICATION", authorId.toString(), start, 600L))
                .thenReturn(new RateLimitStore.Window(start, 1));

        limiter.recordAttempt(authorId);

        verify(store).increment("PUBLICATION", authorId.toString(), start, 600L);
    }
}
