package org.application.repository;

import org.application.model.PublicationImageCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PublicationImageCheckRepository extends JpaRepository<PublicationImageCheck, UUID> {
}
