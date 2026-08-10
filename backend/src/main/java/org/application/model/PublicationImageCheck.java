package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "publication_image_checks", schema = "application")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationImageCheck {

    @Id
    private UUID id;

    @Column(name = "publication_id", nullable = false)
    private UUID publicationId;

    @Column(name = "check_type", nullable = false)
    private String checkType;

    @Column(nullable = false)
    private String provider;

    @Column(name = "attempt_number", nullable = false)
    private Short attemptNumber;

    @Column(nullable = false)
    private String status;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "checked_at", nullable = false)
    private OffsetDateTime checkedAt;
}
