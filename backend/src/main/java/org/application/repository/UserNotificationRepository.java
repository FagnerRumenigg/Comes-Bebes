package org.application.repository;

import org.application.model.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    Page<UserNotification> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<UserNotification> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    List<UserNotification> findByUserIdAndDeletedAtIsNull(UUID userId);
    List<UserNotification> findByUserIdAndReadAtIsNullAndDeletedAtIsNull(UUID userId);
}
