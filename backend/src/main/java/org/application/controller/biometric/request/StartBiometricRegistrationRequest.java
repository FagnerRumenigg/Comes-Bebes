package org.application.controller.biometric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "StartBiometricRegistrationRequest")
public record StartBiometricRegistrationRequest(
        @Schema(description = "Dispositivo (já existente, da IDEIA-019) onde a biometria será registrada.")
        @NotNull UUID deviceId
) {
}
