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

    @Column(length = 280)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_publication_visibility", nullable = false, length = 10)
    @Builder.Default
    private PublicationVisibility defaultPublicationVisibility = PublicationVisibility.PUBLIC;

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

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    // Avisos (docs/telas/09-configuracoes.html, seção "Avisos"). Todos ligados
    // por padrão, exceto "alguém que sigo publica" e o resumo semanal — a
    // própria referência mostra esses dois desligados de cara.
    @Column(name = "notify_on_followed_publish", nullable = false)
    @Builder.Default
    private boolean notifyOnFollowedPublish = false;

    @Column(name = "notify_on_saved", nullable = false)
    @Builder.Default
    private boolean notifyOnSaved = true;

    @Column(name = "notify_on_reacted", nullable = false)
    @Builder.Default
    private boolean notifyOnReacted = true;

    @Column(name = "notify_on_my_version", nullable = false)
    @Builder.Default
    private boolean notifyOnMyVersion = true;

    @Column(name = "notify_on_collection_new_item", nullable = false)
    @Builder.Default
    private boolean notifyOnCollectionNewItem = true;

    @Column(name = "notify_on_collection_shared", nullable = false)
    @Builder.Default
    private boolean notifyOnCollectionShared = true;

    @Column(name = "notify_weekly_email", nullable = false)
    @Builder.Default
    private boolean notifyWeeklyEmail = false;

    @Column(name = "last_seen_patch_note_at")
    private OffsetDateTime lastSeenPatchNoteAt;

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

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updateDefaultPublicationVisibility(PublicationVisibility visibility) {
        this.defaultPublicationVisibility = visibility;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void markPatchNotesSeen(OffsetDateTime seenAt) {
        this.lastSeenPatchNoteAt = seenAt;
    }

    public void updateNotifyOnFollowedPublish(boolean value) {
        this.notifyOnFollowedPublish = value;
    }

    public void updateNotifyOnSaved(boolean value) {
        this.notifyOnSaved = value;
    }

    public void updateNotifyOnReacted(boolean value) {
        this.notifyOnReacted = value;
    }

    public void updateNotifyOnMyVersion(boolean value) {
        this.notifyOnMyVersion = value;
    }

    public void updateNotifyOnCollectionNewItem(boolean value) {
        this.notifyOnCollectionNewItem = value;
    }

    public void updateNotifyOnCollectionShared(boolean value) {
        this.notifyOnCollectionShared = value;
    }

    public void updateNotifyWeeklyEmail(boolean value) {
        this.notifyWeeklyEmail = value;
    }

    public void delete(OffsetDateTime deletedAt) {
        this.status = UserStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public void anonymize() {
        email = null;
        username = "deleted_" + id.toString().replace("-", "").substring(0, 20);
        displayName = "Conta removida";
        bio = null;
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
        if (lastSeenPatchNoteAt == null) lastSeenPatchNoteAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
