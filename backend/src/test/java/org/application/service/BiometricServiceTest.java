package org.application.service;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.application.controller.biometric.request.CompleteBiometricAuthenticationRequest;
import org.application.controller.biometric.request.CompleteBiometricRegistrationRequest;
import org.application.model.BiometricType;
import org.application.model.User;
import org.application.model.UserBiometric;
import org.application.model.UserDevice;
import org.application.repository.UserBiometricRepository;
import org.application.repository.UserDeviceRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiometricServiceTest {
    @Mock private RelyingParty relyingParty;
    @Mock private UserBiometricRepository biometricRepository;
    @Mock private UserDeviceRepository deviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;
    @Mock private Clock clock;
    @InjectMocks private BiometricService service;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(NOW);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

    private UserDevice device(UUID id, UUID userId) {
        return UserDevice.builder().id(id).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(true).trusted(false).build();
    }

    private UserBiometric biometric(UUID id, UUID userId, UUID deviceId) {
        return UserBiometric.builder().id(id).userId(userId).deviceId(deviceId)
                .biometricType(BiometricType.FACE_ID)
                .credentialId(new byte[]{1, 2, 3})
                .publicKeyCose(new byte[]{4, 5, 6})
                .signatureCount(0L)
                .registeredAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(true)
                .build();
    }

    @Test
    void shouldRejectStartRegistrationWhenDeviceNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").displayName("Fagner").build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startRegistration(user, deviceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBuildRegistrationChallengeForOwnedDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").displayName("Fagner").build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device(deviceId, userId)));

        PublicKeyCredentialCreationOptions options = mock(PublicKeyCredentialCreationOptions.class);
        try {
            when(options.toCredentialsCreateJson()).thenReturn("{\"publicKey\":{}}");
            when(options.toJson()).thenReturn("{\"state\":true}");
        } catch (Exception ignored) {
            // toCredentialsCreateJson()/toJson() declaram JsonProcessingException checked
        }
        when(relyingParty.startRegistration(any())).thenReturn(options);

        var response = service.startRegistration(user, deviceId);

        assertThat(response.state()).isEqualTo("{\"state\":true}");
        assertThat(response.publicKeyCredentialCreationOptions().get("publicKey")).isNotNull();
    }

    @Test
    void shouldTranslateRegistrationFailure() throws RegistrationFailedException {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").displayName("Fagner").build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device(deviceId, userId)));

        try (MockedStatic<PublicKeyCredentialCreationOptions> optionsStatic = mockStatic(PublicKeyCredentialCreationOptions.class);
             MockedStatic<PublicKeyCredential> credentialStatic = mockStatic(PublicKeyCredential.class)) {
            optionsStatic.when(() -> PublicKeyCredentialCreationOptions.fromJson(any())).thenReturn(mock(PublicKeyCredentialCreationOptions.class));
            credentialStatic.when(() -> PublicKeyCredential.parseRegistrationResponseJson(any())).thenReturn(mock(PublicKeyCredential.class));
            when(relyingParty.finishRegistration(any())).thenThrow(new RegistrationFailedException(new IllegalArgumentException("bad attestation")));

            var request = new CompleteBiometricRegistrationRequest(deviceId, "state", objectMapper.readTree("{}"), "FACE_ID");

            assertThatThrownBy(() -> service.completeRegistration(user, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .extracting(exception -> ((InvalidOperationException) exception).code())
                    .isEqualTo("BIOMETRIC_REGISTRATION_FAILED");
        }
    }

    @Test
    void shouldRejectRegistrationCompleteWithMalformedState() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").displayName("Fagner").build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device(deviceId, userId)));

        var request = new CompleteBiometricRegistrationRequest(deviceId, "not-valid-json", objectMapper.readTree("{}"), "FACE_ID");

        assertThatThrownBy(() -> service.completeRegistration(user, request))
                .isInstanceOf(InvalidOperationException.class)
                .extracting(exception -> ((InvalidOperationException) exception).code())
                .isEqualTo("BIOMETRIC_INVALID_RESPONSE");
    }

    @Test
    void shouldRejectStartAuthenticationWhenNoBiometricOnDevice() {
        UUID deviceId = UUID.randomUUID();
        when(biometricRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startAuthentication(deviceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBuildAuthenticationChallengeWhenBiometricExists() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(biometricRepository.findByDeviceIdAndActiveTrue(deviceId))
                .thenReturn(Optional.of(biometric(UUID.randomUUID(), userId, deviceId)));

        AssertionRequest assertionRequest = mock(AssertionRequest.class);
        try {
            when(assertionRequest.toCredentialsGetJson()).thenReturn("{\"publicKey\":{}}");
            when(assertionRequest.toJson()).thenReturn("{\"state\":true}");
        } catch (Exception ignored) {
            // toJson()/toCredentialsGetJson() declaram JsonProcessingException checked
        }
        when(relyingParty.startAssertion(any())).thenReturn(assertionRequest);

        var response = service.startAuthentication(deviceId);

        assertThat(response.state()).isEqualTo("{\"state\":true}");
    }

    @Test
    void shouldRejectAuthenticationWhenDeviceIsInactive() {
        UUID deviceId = UUID.randomUUID();
        UserDevice inactive = UserDevice.builder().id(deviceId).userId(UUID.randomUUID())
                .deviceHash("hash").deviceName("Chrome")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(false).trusted(false).build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(inactive));

        var request = new CompleteBiometricAuthenticationRequest(deviceId, "state", objectMapper.readTree("{}"));

        assertThatThrownBy(() -> service.completeAuthentication(request))
                .isInstanceOf(InvalidOperationException.class)
                .extracting(exception -> ((InvalidOperationException) exception).code())
                .isEqualTo("DEVICE_INACTIVE");
    }

    @Test
    void shouldTranslateAssertionFailure() throws AssertionFailedException {
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device(deviceId, UUID.randomUUID())));

        try (MockedStatic<AssertionRequest> requestStatic = mockStatic(AssertionRequest.class);
             MockedStatic<PublicKeyCredential> credentialStatic = mockStatic(PublicKeyCredential.class)) {
            requestStatic.when(() -> AssertionRequest.fromJson(any())).thenReturn(mock(AssertionRequest.class));
            credentialStatic.when(() -> PublicKeyCredential.parseAssertionResponseJson(any())).thenReturn(mock(PublicKeyCredential.class));
            when(relyingParty.finishAssertion(any())).thenThrow(new AssertionFailedException(new IllegalArgumentException("bad signature")));

            var request = new CompleteBiometricAuthenticationRequest(deviceId, "state", objectMapper.readTree("{}"));

            assertThatThrownBy(() -> service.completeAuthentication(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .extracting(exception -> ((InvalidOperationException) exception).code())
                    .isEqualTo("BIOMETRIC_AUTHENTICATION_FAILED");
        }
    }

    @Test
    void shouldFilterBiometricsByDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID otherDeviceId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device(deviceId, userId)));
        when(biometricRepository.findByUserIdAndActiveTrueOrderByRegisteredAtDesc(userId)).thenReturn(List.of(
                biometric(UUID.randomUUID(), userId, deviceId),
                biometric(UUID.randomUUID(), userId, otherDeviceId)));

        var result = service.listBiometrics(user, deviceId);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectRemovingBiometricThatDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID biometricId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").build();
        when(biometricRepository.findByIdAndUserId(biometricId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeBiometric(biometricId, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRevokeOwnedBiometric() {
        UUID userId = UUID.randomUUID();
        UUID biometricId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").build();
        UserBiometric existing = biometric(biometricId, userId, UUID.randomUUID());
        when(biometricRepository.findByIdAndUserId(biometricId, userId)).thenReturn(Optional.of(existing));

        service.removeBiometric(biometricId, user);

        assertThat(existing.isActive()).isFalse();
        verify(biometricRepository).save(existing);
    }

    @Test
    void shouldReportHasBiometric() {
        UUID deviceId = UUID.randomUUID();
        when(biometricRepository.findByDeviceIdAndActiveTrue(deviceId))
                .thenReturn(Optional.of(biometric(UUID.randomUUID(), UUID.randomUUID(), deviceId)));

        assertThat(service.hasBiometric(deviceId)).isTrue();
    }
}
