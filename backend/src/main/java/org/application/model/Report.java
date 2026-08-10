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
@Table(name = "reports", schema = "application")
public class Report {

    @Id
    private UUID id;

    @Column(name = "publication_id", nullable = false)
    private UUID publicationId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "reason_id", nullable = false)
    private Short reasonId;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "moderation_case_id")
    private UUID moderationCaseId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String resolution = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void assignToCase(UUID moderationCaseId) {
        this.moderationCaseId = moderationCaseId;
    }

    public void resolve(String resolution, OffsetDateTime resolvedAt) {
        this.resolution = resolution;
        this.resolvedAt = resolvedAt;
    }
}
