package org.application.repository;

import org.application.model.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionTypeRepository extends JpaRepository<ReactionType, Short> {
    Optional<ReactionType> findByCodeAndActiveTrue(String code);

    /**
     * Sem filtro de {@code active}: usado para remover uma reação já aplicada mesmo que o tipo
     * tenha sido retirado do catálogo depois (produto5.md v5 §6.1) — quem reagiu antes da
     * retirada continua podendo tirar a própria reação.
     */
    Optional<ReactionType> findByCode(String code);
}
