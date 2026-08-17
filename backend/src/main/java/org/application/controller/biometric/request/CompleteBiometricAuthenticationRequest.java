package org.application.controller.biometric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@Schema(name = "CompleteBiometricAuthenticationRequest")
public record CompleteBiometricAuthenticationRequest(
        @NotNull UUID deviceId,
        @Schema(description = "Valor opaco devolvido por /authenticate/start; deve ser reenviado sem alterações.")
        @NotBlank String state,
        @Schema(description = "Resultado de credential.toJSON() no navegador, após o navigator.credentials.get().")
        @NotNull JsonNode credential
) {
}
