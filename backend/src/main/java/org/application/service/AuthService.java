package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.auth.request.LoginRequest;
import org.application.controller.auth.response.LoginResponse;
import org.application.model.PasswordResetToken;
import org.application.model.User;
import org.application.model.UserDevice;
import org.application.model.UserNotification;
import org.application.model.UserStatus;
import org.application.model.RefreshToken;
import org.application.repository.UserRepository;
import org.application.repository.PasswordResetTokenRepository;
import org.application.repository.RefreshTokenRepository;
import org.application.repository.UserNotificationRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.exception.InvalidOperationException;
import org.application.util.StringNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String NEW_DEVICE_LOGIN = "NEW_DEVICE_LOGIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringNormalizer normalizer;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetRateLimiter passwordResetRateLimiter;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PatchNoteService patchNoteService;
    private final DeviceService deviceService;
    private final UserNotificationRepository notificationRepository;
    private final EmailSender emailSender;

    @Value("${app.security.jwt-issuer}")
    private String issuer;
    @Value("${app.security.jwt-expiration-minutes}")
    private long expirationMinutes;
    @Value("${app.security.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;
    @Value("${app.security.device-inactivity-timeout-days}")
    private long deviceInactivityTimeoutDays;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final long PASSWORD_RESET_TOKEN_TTL_MINUTES = 60;

    public LoginResponse login(LoginRequest request) {
        return login(request, request.identifier(), null, null);
    }

    public LoginResponse login(LoginRequest request, String rateLimitKey) {
        return login(request, rateLimitKey, null, null);
    }

    public LoginResponse login(LoginRequest request, String rateLimitKey, String userAgent, String ipAddress) {
        String identifier = normalizer.normalize(request.identifier());
        loginRateLimiter.check(rateLimitKey);
        var userOptional = findByIdentifier(identifier)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()));
        if (userOptional.isEmpty()) {
            loginRateLimiter.registerFailure(rateLimitKey);
            log.warn("event=login_failed rateLimitKey={}", rateLimitKey);
            throw new InvalidOperationException("INVALID_CREDENTIALS", "E-mail, usuário ou senha inválidos.");
        }
        var user = userOptional.get();
        loginRateLimiter.reset(rateLimitKey);

        var deviceLookup = deviceService.getOrCreateDevice(user.getId(), userAgent, ipAddress);
        if (deviceLookup.isNew()) {
            notifyNewDevice(user.getId(), deviceLookup.device());
        }

        LoginResponse response = issueSession(user, UUID.randomUUID(), deviceLookup.device());
        log.info("event=login_success username={} userId={} sessionId={} deviceId={}",
                user.getUsername(), user.getId(), response.sessionId(), deviceLookup.device().getId());
        return response;
    }

    /**
     * Contas sem e-mail (criadas antes da migração — produto5.md v5 §5.1) continuam
     * entrando por @usuário; assim que o e-mail é definido, o @usuário para de valer como
     * login e passa a exigir e-mail — sem manter as duas portas abertas pra sempre.
     */
    private java.util.Optional<User> findByIdentifier(String identifier) {
        var byUsername = userRepository.findByUsernameIgnoreCase(identifier)
                .filter(candidate -> candidate.getEmail() == null);
        if (byUsername.isPresent()) return byUsername;
        return userRepository.findByEmailIgnoreCase(identifier);
    }

    public LoginResponse refresh(String rawRefreshToken) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(token -> token.isUsable(now))
                .orElseThrow(() -> new InvalidOperationException("REFRESH_TOKEN_INVALID", "Refresh token inválido ou expirado."));
        var user = userRepository.findByIdAndStatus(stored.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));

        UserDevice device = requireActiveDevice(stored, now);

        stored.revoke(now);
        refreshTokenRepository.save(stored);
        UUID sessionId = stored.getSessionId() == null ? UUID.randomUUID() : stored.getSessionId();
        deviceService.touchActivity(device);
        LoginResponse response = issueSession(user, sessionId, device);
        log.info("event=session_refresh userId={} sessionId={} deviceId={}", user.getId(), sessionId, device.getId());
        return response;
    }

    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
            refreshTokenRepository.save(token);
            log.info("event=logout userId={} sessionId={}", token.getUserId(), token.getSessionId());
        });
    }

    /**
     * "Esqueceu a senha?" (docs/telas/11-recuperar-senha.html) — sempre "dá certo"
     * do ponto de vista de quem pediu, exista ou não a conta com esse e-mail.
     * Só quem realmente tem conta recebe alguma coisa; não dá pra descobrir por
     * aqui quem tem cadastro (mesmo espírito do texto "Se existir uma conta com...").
     */
    public void requestPasswordReset(String email) {
        String normalized = normalizer.normalize(email);
        passwordResetRateLimiter.recordAttempt(normalized);
        userRepository.findByEmailIgnoreCase(normalized)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::sendPasswordResetEmail);
    }

    private void sendPasswordResetEmail(User user) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        String rawToken = generateRefreshToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(now.plusMinutes(PASSWORD_RESET_TOKEN_TTL_MINUTES))
                .build());
        emailSender.sendPasswordReset(user.getEmail(), frontendUrl + "/recuperar-senha/" + rawToken);
        log.info("event=password_reset_requested userId={}", user.getId());
    }

    /**
     * Ao trocar a senha por aqui, todas as sessões da conta são encerradas
     * (produto5.md — quem trocou a senha esquecida entra de novo em todo lugar,
     * inclusive porque quem pediu pode não ser quem estava logado antes).
     */
    public void resetPassword(String rawToken, String newPassword) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new InvalidOperationException("PASSWORD_RESET_TOKEN_INVALID", "Este link não vale mais."));
        User user = userRepository.findByIdAndStatus(token.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));

        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.markUsed(now);
        passwordResetTokenRepository.save(token);
        logoutAll(user.getId());
        log.info("event=password_reset_completed userId={}", user.getId());
    }

    public void logoutAll(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> {
            token.revoke(now);
            refreshTokenRepository.save(token);
        });
        log.info("event=logout_all userId={}", userId);
    }

    /**
     * Um device inativo (nem sequer os refreshes silenciosos em segundo plano rodaram) por
     * mais de app.security.device-inactivity-timeout-days é revogado — mas usar o app com
     * frequência, mesmo sem digitar a senha de novo, conta como atividade e mantém a sessão viva.
     */
    private UserDevice requireActiveDevice(RefreshToken stored, OffsetDateTime now) {
        UUID deviceId = stored.getDeviceId();
        UserDevice device = deviceId == null ? null : deviceService.find(deviceId).orElse(null);
        if (device == null || !device.isActive()) {
            throw new InvalidOperationException("REFRESH_TOKEN_INVALID", "Refresh token inválido ou expirado.");
        }
        long daysSinceLastActivity = ChronoUnit.DAYS.between(device.getLastActivityAt(), now);
        if (daysSinceLastActivity > deviceInactivityTimeoutDays) {
            stored.revoke(now);
            refreshTokenRepository.save(stored);
            deviceService.revokeDevice(device.getId(), device.getUserId());
            throw new InvalidOperationException("DEVICE_INACTIVE_TIMEOUT",
                    "Você ficou muito tempo sem acessar. Faça login novamente por segurança.");
        }
        return device;
    }

    private void notifyNewDevice(UUID userId, UserDevice device) {
        notificationRepository.save(UserNotification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NEW_DEVICE_LOGIN)
                .build());
        log.info("event=new_device_detected userId={} deviceId={} deviceName={}", userId, device.getId(), device.getDeviceName());
    }

    LoginResponse issueSession(User user, UUID sessionId, UserDevice device) {
        OffsetDateTime issuedAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime expiresAt = issuedAt.plusMinutes(expirationMinutes);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt.toInstant())
                .expiresAt(expiresAt.toInstant())
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("session_id", sessionId.toString())
                .claim("device_id", device.getId().toString())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        String rawRefreshToken = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .sessionId(sessionId)
                .deviceId(device.getId())
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(issuedAt.plusDays(refreshTokenExpirationDays))
                .build());
        return new LoginResponse(token, rawRefreshToken, "Bearer", expirationMinutes * 60, user.getId(), user.getUsername(),
                user.getRole(), user.isOnboardingCompleted(), patchNoteService.hasUnseen(user), user.getEmail() == null,
                expiresAt, sessionId, device.getId());
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algoritmo de hash não disponível.", exception);
        }
    }
}
