package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.auth.request.LoginRequest;
import org.application.controller.auth.response.LoginResponse;
import org.application.model.UserStatus;
import org.application.model.RefreshToken;
import org.application.repository.UserRepository;
import org.application.repository.RefreshTokenRepository;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringNormalizer normalizer;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final LoginRateLimiter loginRateLimiter;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.security.jwt-issuer}")
    private String issuer;
    @Value("${app.security.jwt-expiration-minutes}")
    private long expirationMinutes;
    @Value("${app.security.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    public LoginResponse login(LoginRequest request) {
        return login(request, request.username());
    }

    public LoginResponse login(LoginRequest request, String rateLimitKey) {
        String username = normalizer.normalize(request.username());
        loginRateLimiter.check(rateLimitKey);
        var userOptional = userRepository.findByUsernameIgnoreCase(username)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()));
        if (userOptional.isEmpty()) {
            loginRateLimiter.registerFailure(rateLimitKey);
            log.warn("event=login_failed rateLimitKey={}", rateLimitKey);
            throw new InvalidOperationException("INVALID_CREDENTIALS", "Username ou senha inválidos.");
        }
        var user = userOptional.get();
        loginRateLimiter.reset(rateLimitKey);

        LoginResponse response = issueSession(user, UUID.randomUUID());
        log.info("event=login_success username={} userId={} sessionId={}", user.getUsername(), user.getId(), response.sessionId());
        return response;
    }

    public LoginResponse refresh(String rawRefreshToken) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(token -> token.isUsable(now))
                .orElseThrow(() -> new InvalidOperationException("REFRESH_TOKEN_INVALID", "Refresh token inválido ou expirado."));
        var user = userRepository.findByIdAndStatus(stored.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        stored.revoke(now);
        refreshTokenRepository.save(stored);
        UUID sessionId = stored.getSessionId() == null ? UUID.randomUUID() : stored.getSessionId();
        LoginResponse response = issueSession(user, sessionId);
        log.info("event=session_refresh userId={} sessionId={}", user.getId(), sessionId);
        return response;
    }

    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
            refreshTokenRepository.save(token);
            log.info("event=logout userId={} sessionId={}", token.getUserId(), token.getSessionId());
        });
    }

    private LoginResponse issueSession(org.application.model.User user, UUID sessionId) {
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
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        String rawRefreshToken = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .sessionId(sessionId)
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(issuedAt.plusDays(refreshTokenExpirationDays))
                .build());
        return new LoginResponse(token, rawRefreshToken, "Bearer", expirationMinutes * 60, user.getId(), user.getUsername(), user.getRole(), user.isOnboardingCompleted(), expiresAt, sessionId);
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
