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
@Table(name = "publication_origins", schema = "application")
public class PublicationOrigin {

    @Id
    @Column(name = "derived_publication_id")
    private UUID derivedPublicationId;

    @Column(name = "source_recipe_id")
    private UUID sourceRecipeId;

    @Column(name = "source_title_snapshot", nullable = false, length = 150)
    private String sourceTitleSnapshot;

    @Column(name = "title_suffix", nullable = false, length = 100)
    private String titleSuffix;

    @Column(name = "change_summary", columnDefinition = "text")
    private String changeSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
