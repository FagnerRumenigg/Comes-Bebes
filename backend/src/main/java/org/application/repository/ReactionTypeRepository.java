package org.application.repository;

import org.application.model.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionTypeRepository extends JpaRepository<ReactionType, Short> {
    Optional<ReactionType> findByCodeAndActiveTrue(String code);
}
