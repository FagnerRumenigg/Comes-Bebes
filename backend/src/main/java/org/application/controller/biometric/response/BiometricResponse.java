package org.application.controller.biometric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.UserBiometric;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "BiometricResponse", description = "Biometria registrada em um dispositivo.")
public record BiometricResponse(
        UUID id,
        String biometricType,
        OffsetDateTime registeredAt,
        @Schema(nullable = true) OffsetDateTime lastUsedAt,
        boolean isActive
) {
    public static BiometricResponse of(UserBiometric biometric) {
        return BiometricResponse.builder()
                .id(biometric.getId())
                .biometricType(biometric.getBiometricType().name())
                .registeredAt(biometric.getRegisteredAt())
                .lastUsedAt(biometric.getLastUsedAt())
                .isActive(biometric.isActive())
                .build();
    }
}
