package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.user.request.UpdateUserRequest;
import org.application.controller.user.request.UpdateNotificationPreferencesRequest;
import org.application.controller.user.request.ChangePasswordRequest;
import org.application.dto.UserData;
import org.application.model.PublicationVisibility;
import org.application.model.User;
import org.application.model.UserStatus;
import org.application.model.RetiredUsername;
import org.application.repository.RetiredUsernameRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.DuplicateResourceException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.application.util.StringNormalizer;
import org.application.util.UsernameGenerator;
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
    private static final int USERNAME_RETENTION_DAYS = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringNormalizer stringNormalizer;
    private final Clock clock;
    private final UserNotificationRepository notificationRepository;
    private final RetiredUsernameRepository retiredUsernameRepository;
    private final UsernameGenerator usernameGenerator;

    @Transactional(readOnly = true)
    public Page<UserNotification> notifications(UUID id, Pageable pageable) {
        findActive(id);
        return notificationRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(id, pageable);
    }

    /**
     * "Apagar este aviso" (docs/telas/12-avisos.html) — exclusão lógica, dono
     * só apaga o próprio aviso.
     */
    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        UserNotification notification = notificationRepository
                .findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_NOT_FOUND", "Aviso não encontrado."));
        notification.markDeleted(OffsetDateTime.now(clock));
        notificationRepository.save(notification);
    }

    /**
     * "Limpar tudo" — mesma exclusão lógica, todos os avisos ativos de uma vez.
     */
    @Transactional
    public void clearNotifications(UUID userId) {
        notificationRepository.findByUserIdAndDeletedAtIsNull(userId).forEach(notification -> {
            notification.markDeleted(OffsetDateTime.now(clock));
            notificationRepository.save(notification);
        });
    }

    /**
     * Marca os avisos como lidos ao abrir a tela — o "novo" (borda destacada)
     * da referência é readAt == null.
     */
    @Transactional
    public void markNotificationsRead(UUID userId) {
        notificationRepository.findByUserIdAndReadAtIsNullAndDeletedAtIsNull(userId).forEach(notification -> {
            notification.markRead(OffsetDateTime.now(clock));
            notificationRepository.save(notification);
        });
    }

    /**
     * O @usuário deixou de ser credencial (produto5.md v5 §5.1): não é mais digitado no
     * cadastro, e sim gerado a partir do nome de exibição, com a mesma resolução de
     * colisão da troca (impl10.md v10 §19.4).
     */
    @Transactional
    public User create(CreateUserRequest request) {
        String email = stringNormalizer.normalize(request.email());
        ensureEmailAvailable(email);
        String username = resolveAvailableUsername(request.displayName(), null);

        UserData data = UserData.builder()
                .email(email)
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

        if (request.username() == null && request.displayName() == null && request.email() == null
                && request.bio() == null && request.defaultPublicationVisibility() == null) {
            throw new InvalidOperationException("Informe ao menos um campo para atualização.");
        }

        if (request.email() != null) {
            String email = stringNormalizer.normalize(request.email());
            if (!email.equalsIgnoreCase(user.getEmail())) {
                ensureEmailAvailable(email);
                user.updateEmail(email);
            }
        }

        if (request.username() != null) {
            String currentUsername = user.getUsername();
            String resolved = resolveAvailableUsername(request.username(), currentUsername);
            if (!resolved.equalsIgnoreCase(currentUsername)) {
                user.updateUsername(resolved);
                retiredUsernameRepository.save(RetiredUsername.builder()
                        .id(UUID.randomUUID())
                        .username(currentUsername)
                        .previousOwnerId(user.getId())
                        .build());
            }
        }

        if (request.displayName() != null) {
            user.updateDisplayName(request.displayName().trim());
        }

        if (request.bio() != null) {
            String bio = request.bio().trim();
            user.updateBio(bio.isEmpty() ? null : bio);
        }

        if (request.defaultPublicationVisibility() != null) {
            user.updateDefaultPublicationVisibility(PublicationVisibility.valueOf(request.defaultPublicationVisibility()));
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
    public void updateNotificationPreferences(UUID id, UpdateNotificationPreferencesRequest request) {
        User user = findActive(id);
        if (request.notifyOnFollowedPublish() != null) {
            user.updateNotifyOnFollowedPublish(request.notifyOnFollowedPublish());
        }
        if (request.notifyOnSaved() != null) {
            user.updateNotifyOnSaved(request.notifyOnSaved());
        }
        if (request.notifyOnReacted() != null) {
            user.updateNotifyOnReacted(request.notifyOnReacted());
        }
        if (request.notifyOnMyVersion() != null) {
            user.updateNotifyOnMyVersion(request.notifyOnMyVersion());
        }
        if (request.notifyOnCollectionNewItem() != null) {
            user.updateNotifyOnCollectionNewItem(request.notifyOnCollectionNewItem());
        }
        if (request.notifyOnCollectionShared() != null) {
            user.updateNotifyOnCollectionShared(request.notifyOnCollectionShared());
        }
        if (request.notifyWeeklyEmail() != null) {
            user.updateNotifyWeeklyEmail(request.notifyWeeklyEmail());
        }
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

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("EMAIL_ALREADY_EXISTS", "Este e-mail já está em uso.");
        }
    }

    /**
     * Geração e resolução de colisão do @usuário na troca (impl10.md v10 §19.4, §18.4).
     * Normaliza o valor digitado, e se colidir com alguém, com uma palavra reservada ou
     * com um @ retirado há menos de 30 dias, incrementa um sufixo numérico até achar um
     * livre. O próprio @ atual do usuário nunca conta como colisão.
     */
    private String resolveAvailableUsername(String rawInput, String currentUsername) {
        String base = usernameGenerator.slugify(rawInput);
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(USERNAME_RETENTION_DAYS);
        String candidate = base;
        int suffix = 2;
        while (!isUsernameAvailable(candidate, currentUsername, cutoff)) {
            candidate = withSuffix(base, suffix);
            suffix++;
        }
        return candidate;
    }

    private boolean isUsernameAvailable(String candidate, String currentUsername, OffsetDateTime cutoff) {
        if (candidate.equalsIgnoreCase(currentUsername)) return true;
        if (usernameGenerator.isReserved(candidate)) return false;
        if (userRepository.existsByUsernameIgnoreCase(candidate)) return false;
        return !retiredUsernameRepository.existsByUsernameIgnoreCaseAndRetiredAtAfter(candidate, cutoff);
    }

    private String withSuffix(String base, int suffix) {
        String suffixText = String.valueOf(suffix);
        int maxBaseLength = 20 - suffixText.length();
        String trimmedBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
        return trimmedBase + suffixText;
    }

}
