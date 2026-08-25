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
 * Canal mínimo de "acha que erramos" da tela de Publicar (impl10.md v10 §21.2):
 * quando o validador recusa uma foto por não reconhecer comida, esta é a única forma
 * hoje de descobrir um falso negativo — não existe monitoramento automático do
 * classificador. Ver docs/PLANO_BACKEND_UX.md item 1.4.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "photo_validation_feedback", schema = "application")
public class PhotoValidationFeedback {

    @Id
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
