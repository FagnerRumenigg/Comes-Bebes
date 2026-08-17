package org.application.controller.device.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateDeviceRequest", description = "Campos opcionais para atualizar um dispositivo.")
public record UpdateDeviceRequest(
        @Schema(description = "Novo nome do dispositivo.", example = "iPhone de João", nullable = true)
        @Size(max = 100) String deviceName,
        @Schema(description = "Marca o dispositivo como confiável. Só aceita 'true'; para revogar a confiança, revogue o dispositivo.", example = "true", nullable = true)
        Boolean isTrusted
) {
}
