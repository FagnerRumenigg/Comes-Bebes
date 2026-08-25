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
 * @usuário retirado por troca — fica indisponível para qualquer pessoa por 30 dias
 * (impl10.md v10 §19.4), inclusive para o próprio dono anterior.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "retired_usernames", schema = "application")
public class RetiredUsername {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "previous_owner_id", nullable = false)
    private UUID previousOwnerId;

    @Column(name = "retired_at", nullable = false, updatable = false)
    private OffsetDateTime retiredAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (retiredAt == null) retiredAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
