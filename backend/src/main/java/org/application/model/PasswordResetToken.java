package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Link de "esqueci minha senha" (docs/telas/11-recuperar-senha.html) — vale
 * por 1 hora e é de uso único. Cada pedido novo cria um token novo; não
 * existe "gerar de novo" aqui como no link de convite de coleção, os
 * anteriores simplesmente ficam órfãos até expirar.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens", schema = "application")
public class PasswordResetToken {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
