package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "user_device", schema = "application")
public class UserDevice {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_hash", nullable = false, length = 64)
    private String deviceHash;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_login_at", nullable = false)
    private OffsetDateTime lastLoginAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_trusted", nullable = false)
    private boolean trusted;

    public void registerLogin(String ipAddress, OffsetDateTime now) {
        this.ipAddress = ipAddress;
        lastLoginAt = now;
        lastActivityAt = now;
        active = true;
    }

    public void touchActivity(OffsetDateTime now) {
        lastActivityAt = now;
    }

    public void revoke() {
        active = false;
    }

    public void rename(String newName) {
        deviceName = newName;
    }

    public void trust() {
        trusted = true;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
