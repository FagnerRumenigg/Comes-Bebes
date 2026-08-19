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
@IdClass(CollectionPublicationId.class)
@Table(name = "collection_publications", schema = "application")
public class CollectionPublication {

    @Id
    @Column(name = "collection_id")
    private UUID collectionId;

    @Id
    @Column(name = "publication_id")
    private UUID publicationId;

    @Column(nullable = false)
    private short position;

    @Column(name = "added_at", nullable = false, updatable = false)
    private OffsetDateTime addedAt;

    @PrePersist
    void onCreate() {
        if (addedAt == null) {
            addedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
