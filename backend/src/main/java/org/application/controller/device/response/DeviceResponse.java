package org.application.controller.device.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.UserDevice;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "DeviceResponse", description = "Dispositivo em que o usuário já autenticou.")
public record DeviceResponse(
        UUID id,
        String deviceName,
        OffsetDateTime lastLoginAt,
        OffsetDateTime lastActivityAt,
        boolean isActive,
        boolean isTrusted,
        @Schema(description = "Indica se é o dispositivo usado na requisição atual.")
        boolean isCurrent
) {
    public static DeviceResponse of(UserDevice device, boolean isCurrent) {
        return builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .lastLoginAt(device.getLastLoginAt())
                .lastActivityAt(device.getLastActivityAt())
                .isActive(device.isActive())
                .isTrusted(device.isTrusted())
                .isCurrent(isCurrent)
                .build();
    }
}
