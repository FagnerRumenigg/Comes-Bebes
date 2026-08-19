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
@IdClass(PublicationViewId.class)
@Table(name = "publication_views", schema = "application")
public class PublicationView {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "publication_id")
    private UUID publicationId;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @PrePersist
    void onCreate() {
        if (viewedAt == null) {
            viewedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
