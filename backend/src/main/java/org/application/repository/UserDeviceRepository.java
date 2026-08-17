package org.application.repository;

import org.application.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByUserIdAndDeviceHash(UUID userId, String deviceHash);

    Optional<UserDevice> findByIdAndUserId(UUID id, UUID userId);

    List<UserDevice> findByUserIdOrderByLastLoginAtDesc(UUID userId);
}
