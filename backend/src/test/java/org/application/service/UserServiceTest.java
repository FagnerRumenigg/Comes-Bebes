package org.application.service;

import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.user.request.UpdateNotificationPreferencesRequest;
import org.application.controller.user.request.UpdateUserRequest;
import org.application.controller.user.request.ChangePasswordRequest;
import org.application.model.RetiredUsername;
import org.application.model.User;
import org.application.model.UserStatus;
import org.application.repository.RetiredUsernameRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.util.StringNormalizer;
import org.application.util.UsernameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private RetiredUsernameRepository retiredUsernameRepository;

    @Spy
    private UsernameGenerator usernameGenerator = new UsernameGenerator();

    @InjectMocks
    private UserService userService;

    @org.junit.jupiter.api.BeforeEach
    void setUpClockDefaults() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-08-20T12:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    /**
     * O @usuário deixou de ser credencial (produto5.md v5 §5.1): não é mais digitado no
     * cadastro, e sim gerado a partir do nome de exibição.
     */
    @Test
    void shouldCreateUserWithGeneratedUsernameAndEncodedPassword() {
        CreateUserRequest request = new CreateUserRequest("fagner@exemplo.com.br", "MinhaSenha123!", " Fagner ");

        when(stringNormalizer.normalize("fagner@exemplo.com.br")).thenReturn("fagner@exemplo.com.br");
        when(userRepository.existsByEmailIgnoreCase("fagner@exemplo.com.br")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("fagner")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(request);

        assertThat(user.getEmail()).isEqualTo("fagner@exemplo.com.br");
        assertThat(user.getUsername()).isEqualTo("fagner");
        assertThat(user.getDisplayName()).isEqualTo("Fagner");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSlugifyDisplayNameIntoGeneratedUsername() {
        CreateUserRequest request = new CreateUserRequest("joao@exemplo.com.br", "MinhaSenha123!", "João Editado!!");

        when(stringNormalizer.normalize("joao@exemplo.com.br")).thenReturn("joao@exemplo.com.br");
        when(userRepository.existsByEmailIgnoreCase("joao@exemplo.com.br")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("joao_editado")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(request);

        assertThat(user.getUsername()).isEqualTo("joao_editado");
    }

    @Test
    void shouldAppendSuffixWhenGeneratedUsernameCollidesOnRegistration() {
        CreateUserRequest request = new CreateUserRequest("fagner@exemplo.com.br", "MinhaSenha123!", "Fagner");

        when(stringNormalizer.normalize("fagner@exemplo.com.br")).thenReturn("fagner@exemplo.com.br");
        when(userRepository.existsByEmailIgnoreCase("fagner@exemplo.com.br")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("fagner")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("fagner2")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(request);

        assertThat(user.getUsername()).isEqualTo("fagner2");
    }

    @Test
    void shouldRejectRegistrationWithAlreadyUsedEmail() {
        CreateUserRequest request = new CreateUserRequest("fagner@exemplo.com.br", "MinhaSenha123!", "Fagner");

        when(stringNormalizer.normalize("fagner@exemplo.com.br")).thenReturn("fagner@exemplo.com.br");
        when(userRepository.existsByEmailIgnoreCase("fagner@exemplo.com.br")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(org.application.service.exception.DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
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
        UpdateUserRequest request = new UpdateUserRequest("fagner_cozinha", "Fagner da Cozinha", null, null, null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("fagner_cozinha")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("fagner_cozinha");
        assertThat(updated.getDisplayName()).isEqualTo("Fagner da Cozinha");
        verify(retiredUsernameRepository).save(argThat((RetiredUsername retired) ->
                retired.getUsername().equals("fagner") && retired.getPreviousOwnerId().equals(user.getId())));
    }

    @Test
    void shouldNormalizeMessyInputWhenChangingUsername() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest("João Editado!!", null, null, null, null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("joao_editado")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("joao_editado");
    }

    @Test
    void shouldAppendSuffixWhenNormalizedUsernameCollides() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest("maria", null, null, null, null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("maria")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("maria2")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("maria2");
    }

    @Test
    void shouldNeverResolveToAReservedWord() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest("admin", null, null, null, null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("admin2")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("admin2");
        verify(userRepository, never()).existsByUsernameIgnoreCase("admin");
    }

    @Test
    void shouldTreatRecentlyRetiredUsernameAsUnavailable() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest("maria", null, null, null, null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("maria")).thenReturn(false);
        when(retiredUsernameRepository.existsByUsernameIgnoreCaseAndRetiredAtAfter(
                org.mockito.ArgumentMatchers.eq("maria"), any())).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("maria2")).thenReturn(false);
        when(retiredUsernameRepository.existsByUsernameIgnoreCaseAndRetiredAtAfter(
                org.mockito.ArgumentMatchers.eq("maria2"), any())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("maria2");
    }

    @Test
    void shouldKeepCurrentUsernameWhenResubmittedUnchanged() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest("Fagner", null, null, null, null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getUsername()).isEqualTo("fagner");
        verify(retiredUsernameRepository, never()).save(any());
        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
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

        assertThatThrownBy(() -> userService.update(id, new UpdateUserRequest(null, null, null, null, null)))
                .isInstanceOf(InvalidOperationException.class);
    }

    /**
     * Conta antiga sem e-mail usa este caminho para migrar (produto5.md v5 §5.1) —
     * depois disso o login passa a exigir e-mail em vez de @usuário.
     */
    @Test
    void shouldSetEmailOnLegacyAccountWithoutOne() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, "fagner@exemplo.com.br", null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(stringNormalizer.normalize("fagner@exemplo.com.br")).thenReturn("fagner@exemplo.com.br");
        when(userRepository.existsByEmailIgnoreCase("fagner@exemplo.com.br")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.update(id, request);

        assertThat(updated.getEmail()).isEqualTo("fagner@exemplo.com.br");
    }

    @Test
    void shouldRejectEmailAlreadyUsedBySomeoneElse() {
        UUID id = UUID.randomUUID();
        User user = User.of(org.application.dto.UserData.builder()
                .passwordHash("hash").username("fagner").displayName("Fagner").build());
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, "maria@exemplo.com.br", null);

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(stringNormalizer.normalize("maria@exemplo.com.br")).thenReturn("maria@exemplo.com.br");
        when(userRepository.existsByEmailIgnoreCase("maria@exemplo.com.br")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(id, request))
                .isInstanceOf(org.application.service.exception.DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
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

        userService.updateNotificationPreferences(id,
                new UpdateNotificationPreferencesRequest(false, null, null, null, null, null, null));

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
