package org.application.controller.biometric;

import org.application.config.CurrentUser;
import org.application.controller.ApiExceptionHandler;
import org.application.controller.auth.response.LoginResponse;
import org.application.controller.biometric.response.BiometricAuthenticationStartResponse;
import org.application.controller.biometric.response.BiometricRegistrationStartResponse;
import org.application.controller.biometric.response.BiometricResponse;
import org.application.model.User;
import org.application.model.UserRole;
import org.application.repository.UserRepository;
import org.application.service.BiometricService;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BiometricControllerTest {
    @Mock private BiometricService biometricService;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUser currentUser;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BiometricController(biometricService, userRepository, currentUser))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    private User user(UUID id) {
        return User.builder().id(id).username("fagner").displayName("Fagner").role(UserRole.USER).build();
    }

    @Test
    void shouldStartRegistrationForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(biometricService.startRegistration(any(), eq(deviceId))).thenReturn(
                BiometricRegistrationStartResponse.builder()
                        .publicKeyCredentialCreationOptions(JsonMapper.builder().build().readTree("{\"publicKey\":{}}"))
                        .state("opaque-state")
                        .build());

        mockMvc.perform(post("/auth/biometric/register/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("opaque-state"));
    }

    @Test
    void shouldStartAuthenticationWithoutRequiringAuth() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(biometricService.startAuthentication(deviceId)).thenReturn(
                BiometricAuthenticationStartResponse.builder()
                        .publicKeyCredentialRequestOptions(JsonMapper.builder().build().readTree("{\"publicKey\":{}}"))
                        .state("opaque-state")
                        .build());

        mockMvc.perform(post("/auth/biometric/authenticate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("opaque-state"));
    }

    @Test
    void shouldReturnNotFoundWhenNoBiometricRegisteredForDevice() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(biometricService.startAuthentication(deviceId))
                .thenThrow(new ResourceNotFoundException("BIOMETRIC_NOT_FOUND", "Nenhuma biometria registrada neste dispositivo."));

        mockMvc.perform(post("/auth/biometric/authenticate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BIOMETRIC_NOT_FOUND"));
    }

    @Test
    void shouldCompleteAuthenticationAndReturnLoginResponse() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(biometricService.completeAuthentication(any())).thenReturn(
                new LoginResponse("token", "refresh", "Bearer", 3600, userId, "fagner", UserRole.USER, true, false, false, OffsetDateTime.now(), UUID.randomUUID(), deviceId));

        mockMvc.perform(post("/auth/biometric/authenticate/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\",\"state\":\"opaque\",\"credential\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()));
    }

    @Test
    void shouldReturnBiometricStatus() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(biometricService.hasBiometric(deviceId)).thenReturn(true);

        mockMvc.perform(get("/auth/biometric/status").param("deviceId", deviceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasBiometric").value(true));
    }

    @Test
    void shouldListBiometricsForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(biometricService.listBiometrics(any(), eq(deviceId))).thenReturn(List.of(
                BiometricResponse.builder().id(UUID.randomUUID()).biometricType("FACE_ID")
                        .registeredAt(OffsetDateTime.now()).isActive(true).build()));

        mockMvc.perform(get("/auth/biometric").param("deviceId", deviceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].biometricType").value("FACE_ID"));
    }

    @Test
    void shouldRemoveBiometric() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID biometricId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        mockMvc.perform(delete("/auth/biometric/{id}", biometricId))
                .andExpect(status().isNoContent());

        verify(biometricService).removeBiometric(eq(biometricId), any());
    }
}
