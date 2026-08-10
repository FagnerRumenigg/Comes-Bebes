package org.application.service;

import org.application.controller.auth.request.LoginRequest;
import org.application.model.User;
import org.application.model.UserRole;
import org.application.model.UserStatus;
import org.application.model.RefreshToken;
import org.application.repository.UserRepository;
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
    @InjectMocks private AuthService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "issuer", "comesebebes");
        ReflectionTestUtils.setField(service, "expirationMinutes", 60L);
        ReflectionTestUtils.setField(service, "refreshTokenExpirationDays", 30L);
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-08-08T15:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldIssueTokenForActiveUserWithValidPassword() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("fagner@example.com").username("fagner")
                .passwordHash("hash").role(UserRole.USER).build();
        when(normalizer.normalize(" Fagner ")).thenReturn("fagner");
        when(userRepository.findByUsernameIgnoreCase("fagner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
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
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).userId(userId)
                .tokenHash("ignored").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(stored));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token-2");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.refresh("refresh");

        assertThat(response.accessToken()).isEqualTo("token-2");
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() {
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                .tokenHash("ignored").expiresAt(OffsetDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneOffset.UTC)).build();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(stored));

        service.logout("refresh");

        assertThat(stored.getRevokedAt()).isNotNull();
    }
}
