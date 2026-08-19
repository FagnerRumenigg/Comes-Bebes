package org.application.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Contador de rate limit persistido em {@code application.rate_limits} — sobrevive a
 * restart/scale-to-zero do processo, ao contrário de um {@code ConcurrentHashMap} em memória.
 */
@Component
public class RateLimitStore {

    private final JdbcTemplate jdbcTemplate;

    public RateLimitStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Window(Instant startedAt, int attempts) {}

    /** Leitura pura, sem incrementar — janela expirada conta como 0 tentativas. */
    public Window currentWindow(String scope, String key, Instant now, long windowSeconds) {
        Instant cutoff = now.minusSeconds(windowSeconds);
        return jdbcTemplate.query(
                "SELECT window_started_at, attempts FROM application.rate_limits WHERE scope = ? AND key = ?",
                rs -> {
                    if (!rs.next()) {
                        return new Window(now, 0);
                    }
                    Instant startedAt = rs.getTimestamp("window_started_at").toInstant();
                    if (!startedAt.isAfter(cutoff)) {
                        return new Window(now, 0);
                    }
                    return new Window(startedAt, rs.getInt("attempts"));
                },
                scope, key);
    }

    /**
     * Incrementa atomicamente (UPSERT single-statement — sem race entre threads concorrentes).
     * Roda em transação própria ({@code REQUIRES_NEW}) pra contar a tentativa mesmo que a
     * transação chamadora (ex.: criação de publicação) role back por outro motivo depois.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Window increment(String scope, String key, Instant now, long windowSeconds) {
        Instant cutoff = now.minusSeconds(windowSeconds);
        Timestamp nowTs = Timestamp.from(now);
        Timestamp cutoffTs = Timestamp.from(cutoff);
        return jdbcTemplate.query(
                """
                INSERT INTO application.rate_limits (scope, key, window_started_at, attempts, updated_at)
                VALUES (?, ?, ?, 1, ?)
                ON CONFLICT (scope, key) DO UPDATE SET
                    window_started_at = CASE WHEN application.rate_limits.window_started_at <= ?
                        THEN excluded.window_started_at ELSE application.rate_limits.window_started_at END,
                    attempts = CASE WHEN application.rate_limits.window_started_at <= ?
                        THEN 1 ELSE application.rate_limits.attempts + 1 END,
                    updated_at = excluded.updated_at
                RETURNING window_started_at, attempts
                """,
                rs -> {
                    rs.next();
                    return new Window(rs.getTimestamp("window_started_at").toInstant(), rs.getInt("attempts"));
                },
                scope, key, nowTs, nowTs, cutoffTs, cutoffTs);
    }

    public void clear(String scope, String key) {
        jdbcTemplate.update("DELETE FROM application.rate_limits WHERE scope = ? AND key = ?", scope, key);
    }
}
