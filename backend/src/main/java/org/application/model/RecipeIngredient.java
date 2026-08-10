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

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipe_ingredients", schema = "application")
public class RecipeIngredient {

    @Id
    private UUID id;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(nullable = false)
    private short position;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(length = 255)
    private String note;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public void delete(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
