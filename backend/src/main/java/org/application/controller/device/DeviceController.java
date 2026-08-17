package org.application.controller.device;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.device.request.UpdateDeviceRequest;
import org.application.controller.device.response.DeviceResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.service.DeviceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Devices", description = "Dispositivos e sessões da conta autenticada (IDEIA-019).")
public class DeviceController {

    private final DeviceService deviceService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar dispositivos", description = "Lista os dispositivos em que a conta autenticada já entrou, mais recente primeiro.")
    @ApiResponse(responseCode = "200", description = "Dispositivos retornados.")
    public List<DeviceResponse> list(Authentication authentication) {
        UUID userId = currentUser.id(authentication);
        UUID currentDeviceId = currentUser.deviceId(authentication);
        return deviceService.listDevices(userId).stream()
                .map(device -> DeviceResponse.of(device, device.getId().equals(currentDeviceId)))
                .toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revogar dispositivo", description = "Desativa o dispositivo e encerra todas as sessões vinculadas a ele.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dispositivo revogado."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> revoke(@PathVariable UUID id, Authentication authentication) {
        deviceService.revokeDevice(id, currentUser.id(authentication));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualizar dispositivo", description = "Renomeia o dispositivo e/ou marca-o como confiável.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispositivo atualizado."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DeviceResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceRequest request,
            Authentication authentication
    ) {
        UUID userId = currentUser.id(authentication);
        var device = deviceService.updateDevice(id, userId, request.deviceName(), request.isTrusted());
        UUID currentDeviceId = currentUser.deviceId(authentication);
        return DeviceResponse.of(device, device.getId().equals(currentDeviceId));
    }
}
