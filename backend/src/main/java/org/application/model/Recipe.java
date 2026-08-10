package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipes", schema = "application")
public class Recipe {

    @Id
    @Column(name = "publication_id")
    private java.util.UUID publicationId;

    @Column(name = "yield_quantity", precision = 10, scale = 2)
    private BigDecimal yieldQuantity;

    @Column(name = "yield_unit", length = 50)
    private String yieldUnit;

    @Column(nullable = false, columnDefinition = "text")
    private String instructions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public void update(BigDecimal yieldQuantity, String yieldUnit, String instructions) {
        this.yieldQuantity = yieldQuantity;
        this.yieldUnit = yieldUnit;
        this.instructions = instructions;
        this.deletedAt = null;
    }

    public void deactivate(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
