package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
@IdClass(CollectionFollowId.class)
@Table(name = "collection_follows", schema = "application")
public class CollectionFollow {

    @Id
    @Column(name = "follower_id")
    private UUID followerId;

    @Id
    @Column(name = "collection_id")
    private UUID collectionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isActive() {
        return deletedAt == null;
    }

    public void reactivate() {
        deletedAt = null;
    }

    public void remove(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
