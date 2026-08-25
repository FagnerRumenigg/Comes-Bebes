package org.application.repository;

import org.application.model.User;
import org.application.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByIdAndStatus(UUID id, UserStatus status);

    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameIgnoreCaseAndStatus(String username, UserStatus status);
    Optional<User> findByEmailIgnoreCase(String email);
}
