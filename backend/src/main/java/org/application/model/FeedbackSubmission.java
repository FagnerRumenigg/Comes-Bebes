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
import java.util.UUID;

/**
 * "Falar com a gente" (docs/telas/09-configuracoes.html) — só armazenada,
 * sem tela de leitura/admin por enquanto.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feedback_submissions", schema = "application")
public class FeedbackSubmission {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    }
}
