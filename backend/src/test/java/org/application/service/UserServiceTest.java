package org.application.service;

import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.user.request.UpdateUserRequest;
import org.application.controller.user.request.ChangePasswordRequest;
import org.application.model.User;
import org.application.model.UserStatus;
import org.application.repository.UserRepository;
import org.application.service.exception.DuplicateResourceException;
import org.application.service.exception.InvalidOperationException;
import org.application.util.StringNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringNormalizer stringNormalizer;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserWithNormalizedDataAndEncodedPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "MinhaSenha123!",
                "Fagner_Dev",
                " Fagner "
        );

        when(stringNormalizer.normalize("Fagner_Dev")).thenReturn("fagner_dev");
        when(userRepository.existsByUsernameIgnoreCase("fagner_dev")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(request);

        assertThat(user.getEmail()).isNull();
        assertThat(user.getUsername()).isEqualTo("fagner_dev");
        assertThat(user.getDisplayName()).isEqualTo("Fagner");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldUpdateUsernameAndDisplayName() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email("fagner@example.com")
                .passwordHash("hash")
                .username("fagner")
                .displayName("Fagner")
                .build());
        UpdateUserRequest request = new UpdateUserRequest("fagner_cozinha", "Fagner da Cozinha");

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(stringNormalizer.normalize("fagner_cozinha")).thenReturn("fagner_cozinha");
        when(userRepository.existsByUsernameIgnoreCase("fagner_cozinha")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("fagner_cozinha");
        assertThat(updated.getDisplayName()).isEqualTo("Fagner da Cozinha");
    }

    @Test
    void shouldRejectEmptyUpdate() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email("fagner@example.com")
                .passwordHash("hash")
                .username("fagner")
                .displayName("Fagner")
                .build());

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.update(id, new UpdateUserRequest(null, null)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldSoftDeleteUser() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email("fagner@example.com")
                .passwordHash("hash")
                .username("fagner")
                .displayName("Fagner")
                .build());

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));

        when(clock.instant()).thenReturn(java.time.Instant.parse("2026-08-08T15:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);

        userService.delete(id);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void shouldCompleteOnboardingWhenRequested() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email(null).passwordHash("old-hash").username("fagner").displayName("Fagner").build());
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.completeOnboarding(id);

        assertThat(user.isOnboardingCompleted()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void shouldMarkPatchNotesSeenWhenRequested() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email(null).passwordHash("old-hash").username("fagner").displayName("Fagner").build());
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(clock.instant()).thenReturn(java.time.Instant.parse("2026-08-13T12:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);

        userService.markPatchNotesSeen(id);

        assertThat(user.getLastSeenPatchNoteAt()).isEqualTo(OffsetDateTime.parse("2026-08-13T12:00:00Z"));
        verify(userRepository).save(user);
    }

    @Test
    void shouldUpdateNotifyOnFollowedPublishPreference() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email(null).passwordHash("old-hash").username("fagner").displayName("Fagner").build());
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateNotifyOnFollowedPublish(id, false);

        assertThat(user.isNotifyOnFollowedPublish()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordIsValid() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email(null).passwordHash("old-hash").username("fagner").displayName("Fagner").build());
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(id, new ChangePasswordRequest("old-password", "new-password"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectInvalidCurrentPassword() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .email(null).passwordHash("old-hash").username("fagner").displayName("Fagner").build());
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(id, new ChangePasswordRequest("wrong", "new-password")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("A senha atual é inválida.");
    }
}
