package org.application.controller.device;

import org.application.config.CurrentUser;
import org.application.controller.ApiExceptionHandler;
import org.application.model.UserDevice;
import org.application.service.DeviceService;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {
    @Mock private DeviceService deviceService;
    @Mock private CurrentUser currentUser;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceService, currentUser))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    private UserDevice device(UUID id) {
        return UserDevice.builder().id(id).userId(UUID.randomUUID())
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.now()).lastActivityAt(OffsetDateTime.now())
                .active(true).trusted(false).build();
    }

    @Test
    void shouldListDevicesAndMarkCurrentOne() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID currentDeviceId = UUID.randomUUID();
        UserDevice current = device(currentDeviceId);
        UserDevice other = device(UUID.randomUUID());
        when(currentUser.id(any())).thenReturn(userId);
        when(currentUser.deviceId(any())).thenReturn(currentDeviceId);
        when(deviceService.listDevices(userId)).thenReturn(List.of(current, other));

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isCurrent").value(true))
                .andExpect(jsonPath("$[1].isCurrent").value(false));
    }

    @Test
    void shouldRevokeDevice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(userId);

        mockMvc.perform(delete("/devices/{id}", deviceId))
                .andExpect(status().isNoContent());

        verify(deviceService).revokeDevice(deviceId, userId);
    }

    @Test
    void shouldReturnNotFoundWhenRevokingUnknownDevice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(userId);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("DEVICE_NOT_FOUND", "Dispositivo não encontrado."))
                .when(deviceService).revokeDevice(deviceId, userId);

        mockMvc.perform(delete("/devices/{id}", deviceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEVICE_NOT_FOUND"));
    }

    @Test
    void shouldRenameAndTrustDevice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UserDevice updated = device(deviceId);
        when(currentUser.id(any())).thenReturn(userId);
        when(deviceService.updateDevice(eq(deviceId), eq(userId), eq("Meu notebook"), eq(true))).thenReturn(updated);

        mockMvc.perform(patch("/devices/{id}", deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceName\":\"Meu notebook\",\"isTrusted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deviceId.toString()));
    }
}
