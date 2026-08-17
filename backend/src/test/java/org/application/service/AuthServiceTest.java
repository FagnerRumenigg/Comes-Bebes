package org.application.service;

import org.application.controller.auth.request.LoginRequest;
import org.application.model.User;
import org.application.model.UserDevice;
import org.application.model.UserRole;
import org.application.model.UserStatus;
import org.application.model.RefreshToken;
import org.application.repository.UserRepository;
import org.application.repository.UserNotificationRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.util.StringNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StringNormalizer normalizer;
    @Mock private JwtEncoder jwtEncoder;
    @Mock private Clock clock;
    @Mock private LoginRateLimiter loginRateLimiter;
    @Mock private org.application.repository.RefreshTokenRepository refreshTokenRepository;
    @Mock private PatchNoteService patchNoteService;
    @Mock private DeviceService deviceService;
    @Mock private UserNotificationRepository notificationRepository;
    @InjectMocks private AuthService service;

    private static final Instant NOW = Instant.parse("2026-08-08T15:00:00Z");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "issuer", "comesebebes");
        ReflectionTestUtils.setField(service, "expirationMinutes", 60L);
        ReflectionTestUtils.setField(service, "refreshTokenExpirationDays", 30L);
        ReflectionTestUtils.setField(service, "deviceInactivityTimeoutDays", 7L);
        lenient().when(clock.instant()).thenReturn(NOW);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private UserDevice activeDevice(UUID userId, OffsetDateTime lastActivityAt) {
        return UserDevice.builder().id(UUID.randomUUID()).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(lastActivityAt).lastActivityAt(lastActivityAt)
                .active(true).trusted(false).build();
    }

    @Test
    void shouldIssueTokenForActiveUserWithValidPassword() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("fagner@example.com").username("fagner")
                .passwordHash("hash").role(UserRole.USER).build();
        UserDevice device = activeDevice(userId, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(normalizer.normalize(" Fagner ")).thenReturn("fagner");
        when(userRepository.findByUsernameIgnoreCase("fagner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(deviceService.getOrCreateDevice(userId, null, null))
                .thenReturn(new DeviceService.DeviceLookup(device, false));
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(refreshTokenRepository.save(any(org.application.model.RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.login(new LoginRequest(" Fagner ", "password"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void shouldNotifyOnNewDevice() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").passwordHash("hash").role(UserRole.USER).build();
        UserDevice device = activeDevice(userId, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(normalizer.normalize("fagner")).thenReturn("fagner");
        when(userRepository.findByUsernameIgnoreCase("fagner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(deviceService.getOrCreateDevice(userId, "UA", "1.2.3.4"))
                .thenReturn(new DeviceService.DeviceLookup(device, true));
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.login(new LoginRequest("fagner", "password"), "key", "UA", "1.2.3.4");

        org.mockito.Mockito.verify(notificationRepository).save(any(org.application.model.UserNotification.class));
    }

    @Test
    void shouldRejectInvalidCredentials() {
        when(normalizer.normalize("fagner")).thenReturn("fagner");
        when(userRepository.findByUsernameIgnoreCase("fagner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("fagner", "wrong")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Username ou senha inválidos.");
    }

    @Test
    void shouldRotateRefreshTokenAndIssueNewSession() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").passwordHash("hash").role(UserRole.USER)
                .status(UserStatus.ACTIVE).build();
        UserDevice device = activeDevice(userId, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).userId(userId).deviceId(device.getId())
                .tokenHash("ignored").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(stored));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(deviceService.find(device.getId())).thenReturn(Optional.of(device));
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token-2");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.refresh("refresh");

        assertThat(response.accessToken()).isEqualTo("token-2");
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void shouldRejectRefreshWhenDeviceInactiveForTooLong() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").passwordHash("hash").role(UserRole.USER)
                .status(UserStatus.ACTIVE).build();
        OffsetDateTime longAgo = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusDays(10);
        UserDevice device = activeDevice(userId, longAgo);
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).userId(userId).deviceId(device.getId())
                .tokenHash("ignored").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(stored));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(deviceService.find(device.getId())).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(InvalidOperationException.class)
                .extracting(exception -> ((InvalidOperationException) exception).code())
                .isEqualTo("DEVICE_INACTIVE_TIMEOUT");

        org.mockito.Mockito.verify(deviceService).revokeDevice(device.getId(), userId);
    }

    @Test
    void shouldRejectRefreshWhenDeviceRevoked() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("fagner").passwordHash("hash").role(UserRole.USER)
                .status(UserStatus.ACTIVE).build();
        UserDevice device = UserDevice.builder().id(UUID.randomUUID()).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(false).trusted(false).build();
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).userId(userId).deviceId(device.getId())
                .tokenHash("ignored").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(stored));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(deviceService.find(device.getId())).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(InvalidOperationException.class)
                .extracting(exception -> ((InvalidOperationException) exception).code())
                .isEqualTo("REFRESH_TOKEN_INVALID");
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() {
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                .tokenHash("ignored").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(stored));

        service.logout("refresh");

        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void shouldRevokeAllActiveSessionsOnLogoutAll() {
        UUID userId = UUID.randomUUID();
        RefreshToken first = RefreshToken.builder().id(UUID.randomUUID()).userId(userId)
                .tokenHash("a").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        RefreshToken second = RefreshToken.builder().id(UUID.randomUUID()).userId(userId)
                .tokenHash("b").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)).thenReturn(java.util.List.of(first, second));

        service.logoutAll(userId);

        assertThat(first.getRevokedAt()).isNotNull();
        assertThat(second.getRevokedAt()).isNotNull();
    }
}
