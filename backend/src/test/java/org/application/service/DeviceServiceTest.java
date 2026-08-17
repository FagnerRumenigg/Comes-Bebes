package org.application.service;

import org.application.model.RefreshToken;
import org.application.model.UserDevice;
import org.application.repository.RefreshTokenRepository;
import org.application.repository.UserDeviceRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    @Mock private UserDeviceRepository deviceRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private Clock clock;
    @InjectMocks private DeviceService service;

    private static final Instant NOW = Instant.parse("2026-08-08T15:00:00Z");
    private static final String CHROME_WINDOWS_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36";
    private static final String CHROME_ANDROID_UA =
            "Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36";

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(NOW);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        lenient().when(deviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateNewDeviceWithHumanReadableName() {
        UUID userId = UUID.randomUUID();
        when(deviceRepository.findByUserIdAndDeviceHash(any(), any())).thenReturn(Optional.empty());

        var lookup = service.getOrCreateDevice(userId, CHROME_WINDOWS_UA, "1.2.3.4");

        assertThat(lookup.isNew()).isTrue();
        assertThat(lookup.device().getDeviceName()).isEqualTo("Chrome no Windows");
        assertThat(lookup.device().isActive()).isTrue();
        assertThat(lookup.device().isTrusted()).isFalse();
    }

    @Test
    void shouldDetectAndroidInsteadOfLinuxInUserAgent() {
        UUID userId = UUID.randomUUID();
        when(deviceRepository.findByUserIdAndDeviceHash(any(), any())).thenReturn(Optional.empty());

        var lookup = service.getOrCreateDevice(userId, CHROME_ANDROID_UA, "1.2.3.4");

        assertThat(lookup.device().getDeviceName()).isEqualTo("Chrome no Android");
    }

    @Test
    void shouldReuseExistingDeviceAcrossIpChanges() {
        UUID userId = UUID.randomUUID();
        UserDevice existing = UserDevice.builder().id(UUID.randomUUID()).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusDays(1))
                .lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusDays(1))
                .active(true).trusted(false).build();
        when(deviceRepository.findByUserIdAndDeviceHash(any(), any())).thenReturn(Optional.of(existing));

        var lookup = service.getOrCreateDevice(userId, CHROME_WINDOWS_UA, "9.9.9.9");

        assertThat(lookup.isNew()).isFalse();
        assertThat(lookup.device().getId()).isEqualTo(existing.getId());
        assertThat(lookup.device().getIpAddress()).isEqualTo("9.9.9.9");
        assertThat(lookup.device().getLastLoginAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRevokeDeviceAndCascadeActiveRefreshTokens() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UserDevice device = UserDevice.builder().id(deviceId).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(true).trusted(false).build();
        RefreshToken token = RefreshToken.builder().id(UUID.randomUUID()).userId(userId).deviceId(deviceId)
                .tokenHash("hash").expiresAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusDays(1)).build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device));
        when(refreshTokenRepository.findByDeviceIdAndRevokedAtIsNull(deviceId)).thenReturn(List.of(token));

        service.revokeDevice(deviceId, userId);

        assertThat(device.isActive()).isFalse();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void shouldRejectRevokingDeviceThatDoesNotBelongToUser() {
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeDevice(deviceId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRenameAndTrustDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UserDevice device = UserDevice.builder().id(deviceId).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(true).trusted(false).build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device));

        var updated = service.updateDevice(deviceId, userId, "Meu notebook", true);

        assertThat(updated.getDeviceName()).isEqualTo("Meu notebook");
        assertThat(updated.isTrusted()).isTrue();
    }

    @Test
    void shouldRejectBlankDeviceName() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UserDevice device = UserDevice.builder().id(deviceId).userId(userId)
                .deviceHash("hash").deviceName("Chrome no Windows")
                .lastLoginAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).lastActivityAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .active(true).trusted(false).build();
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.updateDevice(deviceId, userId, "   ", null))
                .isInstanceOf(InvalidOperationException.class);
    }
}
