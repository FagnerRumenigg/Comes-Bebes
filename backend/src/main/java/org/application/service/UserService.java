package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.user.request.UpdateUserRequest;
import org.application.controller.user.request.ChangePasswordRequest;
import org.application.dto.UserData;
import org.application.model.User;
import org.application.model.UserStatus;
import org.application.repository.UserRepository;
import org.application.service.exception.DuplicateResourceException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.application.util.StringNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.application.model.UserNotification;
import org.application.repository.UserNotificationRepository;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringNormalizer stringNormalizer;
    private final Clock clock;
    private final UserNotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<UserNotification> notifications(UUID id, Pageable pageable) {
        findActive(id);
        return notificationRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(id, pageable);
    }

    @Transactional
    public User create(CreateUserRequest request) {
        String username = stringNormalizer.normalize(request.username());

        ensureUsernameAvailable(username);

        UserData data = UserData.builder()
                .passwordHash(passwordEncoder.encode(request.password()))
                .username(username)
                .displayName(request.displayName().trim())
                .build();

        return userRepository.save(User.of(data));
    }

    @Transactional(readOnly = true)
    public User findActive(UUID id) {
        return userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public User findActiveByUsername(String username) {
        return userRepository.findByUsernameIgnoreCaseAndStatus(stringNormalizer.normalize(username), UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest request) {
        User user = findActive(id);

        if (request.username() == null && request.displayName() == null) {
            throw new InvalidOperationException("Informe ao menos um campo para atualização.");
        }

        if (request.username() != null) {
            String username = stringNormalizer.normalize(request.username());
            if (!username.equalsIgnoreCase(user.getUsername())) {
                ensureUsernameAvailable(username);
                user.updateUsername(username);
            }
        }

        if (request.displayName() != null) {
            user.updateDisplayName(request.displayName().trim());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findActive(id);
        user.delete(OffsetDateTime.now(clock));
        userRepository.save(user);
    }

    @Transactional
    public void completeOnboarding(UUID id) {
        User user = findActive(id);
        user.completeOnboarding();
        userRepository.save(user);
    }

    @Transactional
    public void markPatchNotesSeen(UUID id) {
        User user = findActive(id);
        user.markPatchNotesSeen(OffsetDateTime.now(clock));
        userRepository.save(user);
    }

    @Transactional
    public void updateNotifyOnFollowedPublish(UUID id, boolean value) {
        User user = findActive(id);
        user.updateNotifyOnFollowedPublish(value);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = findActive(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidOperationException("CURRENT_PASSWORD_INVALID", "A senha atual é inválida.");
        }
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("USERNAME_ALREADY_EXISTS", "Nome de usuário já está cadastrado.");
        }
    }

}
