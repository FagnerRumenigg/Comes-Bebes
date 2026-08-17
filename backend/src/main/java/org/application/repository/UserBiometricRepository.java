package org.application.repository;

import org.application.model.UserBiometric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBiometricRepository extends JpaRepository<UserBiometric, UUID> {

    Optional<UserBiometric> findByCredentialId(byte[] credentialId);

    Optional<UserBiometric> findByDeviceIdAndActiveTrue(UUID deviceId);

    List<UserBiometric> findByUserIdAndActiveTrueOrderByRegisteredAtDesc(UUID userId);

    Optional<UserBiometric> findByIdAndUserId(UUID id, UUID userId);
}
