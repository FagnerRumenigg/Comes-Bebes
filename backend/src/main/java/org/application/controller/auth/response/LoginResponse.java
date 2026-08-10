package org.application.controller.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.application.model.UserRole;

@Schema(name = "LoginResponse", description = "Token Bearer e dados básicos da sessão.")
public record LoginResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
        @Schema(description = "Token opaco usado para renovar a sessão.") String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "3600") long expiresInSeconds,
        UUID userId,
        String username,
        UserRole role,
        boolean onboardingCompleted,
        OffsetDateTime expiresAt,
        UUID sessionId
) {
    public LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds,
                         UUID userId, String username, UserRole role, boolean onboardingCompleted, OffsetDateTime expiresAt) {
        this(accessToken, refreshToken, tokenType, expiresInSeconds, userId, username, role, onboardingCompleted, expiresAt, UUID.randomUUID());
    }
}
