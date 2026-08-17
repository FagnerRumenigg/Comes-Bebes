package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.UserDevice;
import org.application.repository.RefreshTokenRepository;
import org.application.repository.UserDeviceRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final UserDeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    private static final Pattern BROWSER_PATTERN =
            Pattern.compile("(Edg|OPR|Chrome|Firefox|Safari)/");
    private static final Pattern OS_PATTERN =
            Pattern.compile("(Windows|Mac OS X|iPhone|iPad|Linux)");

    public record DeviceLookup(UserDevice device, boolean isNew) {
    }

    /**
     * A identidade do dispositivo é o navegador/OS (user agent), não o IP: o IP muda com
     * frequência (rede móvel, trocas de Wi-Fi) e usá-lo na identidade faria o mesmo aparelho
     * virar "novo dispositivo" a cada troca de rede.
     */
    public DeviceLookup getOrCreateDevice(UUID userId, String userAgent, String ipAddress) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        String hash = hash(userAgent == null ? "" : userAgent);

        Optional<UserDevice> existing = deviceRepository.findByUserIdAndDeviceHash(userId, hash);
        if (existing.isPresent()) {
            UserDevice device = existing.get();
            device.registerLogin(ipAddress, now);
            deviceRepository.save(device);
            return new DeviceLookup(device, false);
        }

        UserDevice device = UserDevice.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceHash(hash)
                .deviceName(deriveDeviceName(userAgent))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .lastLoginAt(now)
                .lastActivityAt(now)
                .active(true)
                .trusted(false)
                .build();
        deviceRepository.save(device);
        return new DeviceLookup(device, true);
    }

    public Optional<UserDevice> find(UUID deviceId) {
        return deviceRepository.findById(deviceId);
    }

    public void touchActivity(UserDevice device) {
        device.touchActivity(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        deviceRepository.save(device);
    }

    public List<UserDevice> listDevices(UUID userId) {
        return deviceRepository.findByUserIdOrderByLastLoginAtDesc(userId);
    }

    public void revokeDevice(UUID deviceId, UUID userId) {
        UserDevice device = requireOwned(deviceId, userId);
        device.revoke();
        deviceRepository.save(device);

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        refreshTokenRepository.findByDeviceIdAndRevokedAtIsNull(deviceId)
                .forEach(token -> {
                    token.revoke(now);
                    refreshTokenRepository.save(token);
                });
    }

    public UserDevice updateDevice(UUID deviceId, UUID userId, String deviceName, Boolean trusted) {
        UserDevice device = requireOwned(deviceId, userId);
        if (deviceName != null) {
            String trimmed = deviceName.trim();
            if (trimmed.isEmpty()) {
                throw new InvalidOperationException("DEVICE_NAME_BLANK", "O nome do dispositivo não pode ficar em branco.");
            }
            device.rename(trimmed);
        }
        if (Boolean.TRUE.equals(trusted)) {
            device.trust();
        }
        deviceRepository.save(device);
        return device;
    }

    private UserDevice requireOwned(UUID deviceId, UUID userId) {
        return deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("DEVICE_NOT_FOUND", "Dispositivo não encontrado."));
    }

    private String deriveDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Dispositivo desconhecido";
        }
        String browser = firstMatch(BROWSER_PATTERN, userAgent, 1)
                .map(match -> switch (match) {
                    case "Edg" -> "Edge";
                    case "OPR" -> "Opera";
                    default -> match;
                })
                .orElse("Navegador");
        return browser + " no " + detectOs(userAgent);
    }

    /**
     * User agents Android trazem "Linux" antes de "Android" (ex.: "Linux; Android 10; ..."),
     * e como Matcher.find() retorna o match mais à esquerda, um regex único sempre acharia
     * "Linux" primeiro independente da ordem da alternação. Por isso o caso Android é checado
     * separadamente, com prioridade sobre o restante do OS_PATTERN.
     */
    private String detectOs(String userAgent) {
        if (userAgent.contains("Android")) {
            return "Android";
        }
        return firstMatch(OS_PATTERN, userAgent, 1)
                .map(match -> switch (match) {
                    case "Mac OS X" -> "Mac";
                    case "iPhone" -> "iOS";
                    case "iPad" -> "iPadOS";
                    default -> match;
                })
                .orElse("dispositivo desconhecido");
    }

    private Optional<String> firstMatch(Pattern pattern, String input, int group) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Optional.of(matcher.group(group)) : Optional.empty();
    }

    private String hash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algoritmo de hash não disponível.", exception);
        }
    }
}
