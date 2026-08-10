package org.application.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
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
@IdClass(PublicationReactionId.class)
@Table(name = "publication_reactions", schema = "application")
public class PublicationReaction {

    @Id
    @Column(name = "publication_id")
    private UUID publicationId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "reaction_type_id")
    private Short reactionTypeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public void reactivate() {
        deletedAt = null;
    }

    public void remove(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
