package org.application.controller.biometric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@Schema(name = "CompleteBiometricRegistrationRequest")
public record CompleteBiometricRegistrationRequest(
        @NotNull UUID deviceId,
        @Schema(description = "Valor opaco devolvido por /register/start; deve ser reenviado sem alterações.")
        @NotBlank String state,
        @Schema(description = "Resultado de credential.toJSON() no navegador, após o navigator.credentials.create().")
        @NotNull JsonNode credential,
        @Schema(description = "Rótulo detectado no navegador (FACE_ID, FINGERPRINT, WINDOWS_HELLO, UNKNOWN) — usado só como texto de exibição.", nullable = true)
        String biometricType
) {
}
