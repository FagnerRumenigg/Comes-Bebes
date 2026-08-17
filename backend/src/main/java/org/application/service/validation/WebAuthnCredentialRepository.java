package org.application.service.validation;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.RequiredArgsConstructor;
import org.application.model.User;
import org.application.repository.UserBiometricRepository;
import org.application.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapta as tabelas de usuário/biometria da aplicação para a interface que o
 * java-webauthn-server espera. "username" aqui é sempre o username da conta, e
 * "userHandle" é o UUID do usuário convertido pra 16 bytes — não guardamos um
 * identificador WebAuthn separado.
 */
@Component
@RequiredArgsConstructor
public class WebAuthnCredentialRepository implements CredentialRepository {

    private final UserRepository userRepository;
    private final UserBiometricRepository biometricRepository;

    public static ByteArray userHandleOf(UUID userId) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(userId.getMostSignificantBits());
        buffer.putLong(userId.getLeastSignificantBits());
        return new ByteArray(buffer.array());
    }

    public static UUID userIdOf(ByteArray userHandle) {
        ByteBuffer buffer = ByteBuffer.wrap(userHandle.getBytes());
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(user -> biometricRepository.findByUserIdAndActiveTrueOrderByRegisteredAtDesc(user.getId()).stream()
                        .map(biometric -> PublicKeyCredentialDescriptor.builder()
                                .id(new ByteArray(biometric.getCredentialId()))
                                .build())
                        .collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).map(User::getId).map(WebAuthnCredentialRepository::userHandleOf);
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return userRepository.findById(userIdOf(userHandle)).map(User::getUsername);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return biometricRepository.findByCredentialId(credentialId.getBytes())
                .filter(biometric -> biometric.isActive() && biometric.getUserId().equals(userIdOf(userHandle)))
                .map(biometric -> RegisteredCredential.builder()
                        .credentialId(new ByteArray(biometric.getCredentialId()))
                        .userHandle(userHandleOf(biometric.getUserId()))
                        .publicKeyCose(new ByteArray(biometric.getPublicKeyCose()))
                        .signatureCount(biometric.getSignatureCount())
                        .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return biometricRepository.findByCredentialId(credentialId.getBytes())
                .filter(org.application.model.UserBiometric::isActive)
                .map(biometric -> Set.of(RegisteredCredential.builder()
                        .credentialId(new ByteArray(biometric.getCredentialId()))
                        .userHandle(userHandleOf(biometric.getUserId()))
                        .publicKeyCose(new ByteArray(biometric.getPublicKeyCose()))
                        .signatureCount(biometric.getSignatureCount())
                        .build()))
                .orElseGet(Set::of);
    }
}
