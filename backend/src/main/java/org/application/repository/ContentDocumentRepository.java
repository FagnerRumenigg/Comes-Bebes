package org.application.repository;

import org.application.model.ContentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContentDocumentRepository extends JpaRepository<ContentDocument, UUID> {
    Optional<ContentDocument> findBySlug(String slug);
}
