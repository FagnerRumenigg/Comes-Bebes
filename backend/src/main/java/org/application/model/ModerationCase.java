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
@Table(name = "moderation_cases", schema = "application")
public class ModerationCase {

    @Id
    private UUID id;

    @Column(name = "publication_id", nullable = false)
    private UUID publicationId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "report_count_at_open", nullable = false)
    private int reportCountAtOpen;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "decision_note", columnDefinition = "text")
    private String decisionNote;

    public void decide(String status, UUID reviewedBy, OffsetDateTime reviewedAt, String decisionNote) {
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.decisionNote = decisionNote;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        openedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
