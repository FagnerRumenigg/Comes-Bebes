package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.application.dto.UserData;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "blocked_username_hmac", length = 64)
    private String blockedUsernameHmac;
    @Column(name = "blocked_by")
    private UUID blockedBy;
    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;
    @Column(name = "block_reason", columnDefinition = "text")
    private String blockReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "show_reaction_counts", nullable = false)
    @Builder.Default
    private boolean showReactionCounts = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static User of(UserData data) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(data.email())
                .passwordHash(data.passwordHash())
                .username(data.username())
                .displayName(data.displayName())
                .build();
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void delete(OffsetDateTime deletedAt) {
        this.status = UserStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public void anonymize() {
        email = null;
        username = "deleted_" + id.toString().replace("-", "").substring(0, 20);
        displayName = "Conta removida";
        passwordHash = "INVALIDATED";
        status = UserStatus.DELETED;
        deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
        blockedUsernameHmac = null;
        email = null;
    }

    public void block(UUID administratorId, OffsetDateTime blockedAt, String reason, String usernameHmac, String anonymizedUsername) {
        blockedUsernameHmac = usernameHmac;
        email = null;
        username = anonymizedUsername;
        passwordHash = "INVALIDATED";
        status = UserStatus.BLOCKED;
        blockedBy = administratorId;
        this.blockedAt = blockedAt;
        blockReason = reason;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
