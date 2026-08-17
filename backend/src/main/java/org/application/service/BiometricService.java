package org.application.service;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.RequiredArgsConstructor;
import org.application.controller.auth.response.LoginResponse;
import org.application.controller.biometric.request.CompleteBiometricAuthenticationRequest;
import org.application.controller.biometric.request.CompleteBiometricRegistrationRequest;
import org.application.controller.biometric.response.BiometricAuthenticationStartResponse;
import org.application.controller.biometric.response.BiometricRegistrationStartResponse;
import org.application.controller.biometric.response.BiometricResponse;
import org.application.model.BiometricType;
import org.application.model.User;
import org.application.model.UserBiometric;
import org.application.model.UserDevice;
import org.application.model.UserStatus;
import org.application.repository.UserBiometricRepository;
import org.application.repository.UserDeviceRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.validation.WebAuthnCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BiometricService {
    private static final Logger log = LoggerFactory.getLogger(BiometricService.class);

    private final RelyingParty relyingParty;
    private final UserBiometricRepository biometricRepository;
    private final UserDeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BiometricRegistrationStartResponse startRegistration(User user, UUID deviceId) {
        requireOwnedDevice(deviceId, user.getId());

        UserIdentity userIdentity = UserIdentity.builder()
                .name(user.getUsername())
                .displayName(user.getDisplayName())
                .id(WebAuthnCredentialRepository.userHandleOf(user.getId()))
                .build();

        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                                .residentKey(ResidentKeyRequirement.DISCOURAGED)
                                .userVerification(UserVerificationRequirement.REQUIRED)
                                .build())
                        .build());

        return BiometricRegistrationStartResponse.builder()
                .publicKeyCredentialCreationOptions(parseJson(toJsonUnchecked(options::toCredentialsCreateJson)))
                .state(toJsonUnchecked(options::toJson))
                .build();
    }

    @Transactional
    public BiometricResponse completeRegistration(User user, CompleteBiometricRegistrationRequest request) {
        UserDevice device = requireOwnedDevice(request.deviceId(), user.getId());

        PublicKeyCredentialCreationOptions options;
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential;
        try {
            options = PublicKeyCredentialCreationOptions.fromJson(request.state());
            credential = PublicKeyCredential.parseRegistrationResponseJson(request.credential().toString());
        } catch (Exception exception) {
            throw new InvalidOperationException("BIOMETRIC_INVALID_RESPONSE", "Resposta de biometria inválida ou expirada — tente novamente.");
        }

        RegistrationResult result;
        try {
            result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options)
                    .response(credential)
                    .build());
        } catch (RegistrationFailedException exception) {
            log.warn("event=biometric_registration_failed userId={} deviceId={}", user.getId(), device.getId(), exception);
            throw new InvalidOperationException("BIOMETRIC_REGISTRATION_FAILED", "Não foi possível registrar a biometria.");
        }

        // Só uma biometria ativa por dispositivo — registrar outra substitui a anterior.
        biometricRepository.findByDeviceIdAndActiveTrue(device.getId()).ifPresent(existing -> {
            existing.revoke();
            biometricRepository.save(existing);
        });

        UserBiometric biometric = UserBiometric.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .deviceId(device.getId())
                .biometricType(parseBiometricType(request.biometricType()))
                .credentialId(result.getKeyId().getId().getBytes())
                .publicKeyCose(result.getPublicKeyCose().getBytes())
                .signatureCount(result.getSignatureCount())
                .active(true)
                .build();
        biometricRepository.save(biometric);
        log.info("event=biometric_registered userId={} deviceId={} biometricId={}", user.getId(), device.getId(), biometric.getId());
        return BiometricResponse.of(biometric);
    }

    @Transactional(readOnly = true)
    public BiometricAuthenticationStartResponse startAuthentication(UUID deviceId) {
        UserBiometric biometric = biometricRepository.findByDeviceIdAndActiveTrue(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("BIOMETRIC_NOT_FOUND", "Nenhuma biometria registrada neste dispositivo."));

        AssertionRequest assertionRequest = relyingParty.startAssertion(
                StartAssertionOptions.builder()
                        .userHandle(WebAuthnCredentialRepository.userHandleOf(biometric.getUserId()))
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build());

        return BiometricAuthenticationStartResponse.builder()
                .publicKeyCredentialRequestOptions(parseJson(toJsonUnchecked(assertionRequest::toCredentialsGetJson)))
                .state(toJsonUnchecked(assertionRequest::toJson))
                .build();
    }

    @Transactional
    public LoginResponse completeAuthentication(CompleteBiometricAuthenticationRequest request) {
        UserDevice device = deviceRepository.findById(request.deviceId())
                .filter(UserDevice::isActive)
                .orElseThrow(() -> new InvalidOperationException("DEVICE_INACTIVE", "Dispositivo revogado ou inexistente."));

        AssertionRequest assertionRequest;
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential;
        try {
            assertionRequest = AssertionRequest.fromJson(request.state());
            credential = PublicKeyCredential.parseAssertionResponseJson(request.credential().toString());
        } catch (Exception exception) {
            throw new InvalidOperationException("BIOMETRIC_INVALID_RESPONSE", "Resposta de biometria inválida ou expirada — tente novamente.");
        }

        AssertionResult result;
        try {
            result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(credential)
                    .build());
        } catch (AssertionFailedException exception) {
            log.warn("event=biometric_authentication_failed deviceId={}", request.deviceId(), exception);
            throw new InvalidOperationException("BIOMETRIC_AUTHENTICATION_FAILED", "Não foi possível autenticar com a biometria.");
        }

        if (!result.isSuccess()) {
            throw new InvalidOperationException("BIOMETRIC_AUTHENTICATION_FAILED", "Não foi possível autenticar com a biometria.");
        }

        UserBiometric biometric = biometricRepository.findByCredentialId(result.getCredential().getCredentialId().getBytes())
                .filter(UserBiometric::isActive)
                .orElseThrow(() -> new InvalidOperationException("BIOMETRIC_AUTHENTICATION_FAILED", "Biometria não reconhecida."));

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        biometric.recordUsage(result.getSignatureCount(), now);
        biometricRepository.save(biometric);
        device.touchActivity(now);
        deviceRepository.save(device);

        User user = userRepository.findByIdAndStatus(biometric.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));

        LoginResponse response = authService.issueSession(user, UUID.randomUUID(), device);
        log.info("event=biometric_login_success userId={} deviceId={}", user.getId(), device.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public List<BiometricResponse> listBiometrics(User user, UUID deviceId) {
        requireOwnedDevice(deviceId, user.getId());
        return biometricRepository.findByUserIdAndActiveTrueOrderByRegisteredAtDesc(user.getId()).stream()
                .filter(biometric -> biometric.getDeviceId().equals(deviceId))
                .map(BiometricResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeBiometric(UUID biometricId, User user) {
        UserBiometric biometric = biometricRepository.findByIdAndUserId(biometricId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BIOMETRIC_NOT_FOUND", "Biometria não encontrada."));
        biometric.revoke();
        biometricRepository.save(biometric);
    }

    @Transactional(readOnly = true)
    public boolean hasBiometric(UUID deviceId) {
        return biometricRepository.findByDeviceIdAndActiveTrue(deviceId).isPresent();
    }

    private UserDevice requireOwnedDevice(UUID deviceId, UUID userId) {
        return deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("DEVICE_NOT_FOUND", "Dispositivo não encontrado."));
    }

    private BiometricType parseBiometricType(String rawType) {
        if (rawType == null) {
            return BiometricType.UNKNOWN;
        }
        try {
            return BiometricType.valueOf(rawType);
        } catch (IllegalArgumentException exception) {
            return BiometricType.UNKNOWN;
        }
    }

    private JsonNode parseJson(String rawJson) {
        return objectMapper.readTree(rawJson);
    }

    /**
     * options.toJson()/toCredentialsCreateJson() (Yubico) declaram JsonProcessingException
     * checked, mas aqui estamos serializando um objeto que acabamos de criar — uma falha real
     * seria um bug na biblioteca, não algo recuperável em runtime.
     */
    private String toJsonUnchecked(ThrowingJsonSupplier supplier) {
        try {
            return supplier.get();
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar desafio WebAuthn.", exception);
        }
    }

    @FunctionalInterface
    private interface ThrowingJsonSupplier {
        String get() throws com.fasterxml.jackson.core.JsonProcessingException;
    }
}
