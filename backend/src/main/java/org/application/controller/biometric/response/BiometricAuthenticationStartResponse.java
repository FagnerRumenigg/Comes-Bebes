package org.application.controller.biometric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tools.jackson.databind.JsonNode;

@Builder
@Schema(name = "BiometricAuthenticationStartResponse")
public record BiometricAuthenticationStartResponse(
        @Schema(description = "Passe direto para PublicKeyCredential.parseRequestOptionsFromJSON() no navegador.")
        JsonNode publicKeyCredentialRequestOptions,
        @Schema(description = "Valor opaco: reenvie sem alterações em /authenticate/complete.")
        String state
) {
}
