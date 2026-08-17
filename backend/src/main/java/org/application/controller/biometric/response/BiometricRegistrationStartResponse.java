package org.application.controller.biometric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tools.jackson.databind.JsonNode;

@Builder
@Schema(name = "BiometricRegistrationStartResponse")
public record BiometricRegistrationStartResponse(
        @Schema(description = "Passe direto para PublicKeyCredential.parseCreationOptionsFromJSON() no navegador.")
        JsonNode publicKeyCredentialCreationOptions,
        @Schema(description = "Valor opaco: reenvie sem alterações em /register/complete.")
        String state
) {
}
