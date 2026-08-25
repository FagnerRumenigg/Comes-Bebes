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

/**
 * Convite por link secreto para uma coleção "Para quem eu escolher" (produto5.md v5 §6.3).
 * O link precisa ser impossível de adivinhar e poder ser trocado a qualquer momento —
 * "gerar novo link" revoga o convite ativo em vez de acumular.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collection_invites", schema = "application")
public class CollectionInvite {

    @Id
    private UUID id;

    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;

    @Column(nullable = false, length = 43)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    public boolean isActive() {
        return revokedAt == null;
    }

    public void revoke(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
