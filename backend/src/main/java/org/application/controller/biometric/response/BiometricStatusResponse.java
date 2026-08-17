package org.application.controller.biometric.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BiometricStatusResponse")
public record BiometricStatusResponse(
        @Schema(description = "Indica se este dispositivo já tem biometria registrada.")
        boolean hasBiometric
) {
}
