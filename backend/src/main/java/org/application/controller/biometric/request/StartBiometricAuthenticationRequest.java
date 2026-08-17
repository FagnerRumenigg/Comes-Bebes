package org.application.controller.biometric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "StartBiometricAuthenticationRequest")
public record StartBiometricAuthenticationRequest(
        @NotNull UUID deviceId
) {
}
