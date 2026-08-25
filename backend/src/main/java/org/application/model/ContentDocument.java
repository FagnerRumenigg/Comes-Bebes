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
 * Termos de Serviço, Política de Privacidade e FAQ (docs/telas/09-configuracoes.html,
 * seção "Ajuda e sobre"). Linhas mantidas direto no banco — sem endpoint de
 * escrita, mesmo padrão de {@link PatchNote}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_documents", schema = "application")
public class ContentDocument {

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String slug;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
