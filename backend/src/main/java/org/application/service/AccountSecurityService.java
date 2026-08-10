package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.repository.UserRepository;
import org.application.model.UserRole;
import org.application.util.StringNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.exception.InvalidOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountSecurityService {
    private final UserRepository userRepository;
    private final StringNormalizer normalizer;
    private final Clock clock;
    @Value("${app.security.blocked-username-hmac-secret}")
    private String hmacSecret;

    @Transactional
    public void block(UUID userId, UUID administratorId, String reason) {
        var admin = userRepository.findById(administratorId).filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new InvalidOperationException("ADMIN_REQUIRED", "Somente ADMIN pode bloquear usuários."));
        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        if (admin.getId().equals(user.getId())) throw new InvalidOperationException("SELF_BLOCK_FORBIDDEN", "Não é possível bloquear a própria conta.");
        String hmac = hmac(normalizer.normalize(user.getUsername()));
        String compactId = userId.toString().replace("-", "");
        user.block(administratorId, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC), reason, hmac,
                "blocked_" + compactId.substring(0, 20));
        userRepository.save(user);
    }

    @Transactional
    public void anonymize(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        user.anonymize();
        userRepository.save(user);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao gerar HMAC do username.", exception);
        }
    }
}
