package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_biometric", schema = "application")
public class UserBiometric {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "biometric_type", nullable = false, length = 20)
    private BiometricType biometricType;

    @Column(name = "credential_id", nullable = false)
    private byte[] credentialId;

    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    private long signatureCount;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public void recordUsage(long newSignatureCount, OffsetDateTime now) {
        this.signatureCount = newSignatureCount;
        this.lastUsedAt = now;
    }

    public void revoke() {
        this.active = false;
    }

    @PrePersist
    void onCreate() {
        registeredAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
