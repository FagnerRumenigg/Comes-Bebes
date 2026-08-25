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
        @Schema(description = "Indica se existem notas de versão publicadas ainda não vistas pelo usuário.")
        boolean hasUnseenPatchNotes,
        @Schema(description = "Conta ainda sem e-mail cadastrado (criada antes da migração — produto5.md v5 "
                + "§5.1). O frontend deve pedir o e-mail antes de liberar o resto do app; a partir de então "
                + "o login desta conta passa a exigir e-mail.")
        boolean emailRequired,
        OffsetDateTime expiresAt,
        UUID sessionId,
        @Schema(description = "Dispositivo vinculado a esta sessão. Guarde-o para permitir login por biometria neste dispositivo mais tarde.")
        UUID deviceId
) {
    public LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds,
                         UUID userId, String username, UserRole role, boolean onboardingCompleted,
                         boolean hasUnseenPatchNotes, boolean emailRequired, OffsetDateTime expiresAt) {
        this(accessToken, refreshToken, tokenType, expiresInSeconds, userId, username, role, onboardingCompleted,
                hasUnseenPatchNotes, emailRequired, expiresAt, UUID.randomUUID(), UUID.randomUUID());
    }
}
