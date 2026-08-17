package org.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "publications", schema = "application")
public class Publication {

    @Id
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicationVisibility visibility;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "gcs_bucket", nullable = false, length = 222)
    private String gcsBucket;

    @Column(name = "gcs_object_name", nullable = false, length = 1024)
    private String gcsObjectName;

    @Column(name = "gcs_generation", nullable = false)
    private Long gcsGeneration;

    @Column(name = "image_format", nullable = false, length = 20)
    private String imageFormat;

    @Column(name = "image_size_bytes", nullable = false)
    private Long imageSizeBytes;

    @Column(name = "image_width", nullable = false)
    private Integer imageWidth;

    @Column(name = "image_height", nullable = false)
    private Integer imageHeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PublicationStatus status = PublicationStatus.PENDING_VALIDATION;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "edited_by_admin_id")
    private UUID editedByAdminId;

    @Column(name = "edited_by_admin_at")
    private OffsetDateTime editedByAdminAt;

    @Column(name = "photo_taken_at")
    private OffsetDateTime photoTakenAt;

    public void activate(OffsetDateTime publishedAt) {
        this.status = PublicationStatus.ACTIVE;
        this.publishedAt = publishedAt;
    }

    public void updateText(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void changeVisibility(PublicationVisibility visibility) {
        this.visibility = visibility;
    }

    public void changeType(PublicationType type) {
        this.type = type;
    }

    public void remove(OffsetDateTime deletedAt) {
        this.status = PublicationStatus.REMOVED;
        this.deletedAt = deletedAt;
    }

    public void changeStatus(PublicationStatus status) {
        this.status = status;
    }

    public void markEditedByAdmin(UUID adminId, OffsetDateTime at) {
        this.editedByAdminId = adminId;
        this.editedByAdminAt = at;
    }

    public void recordPhotoTakenAt(OffsetDateTime photoTakenAt) {
        this.photoTakenAt = photoTakenAt;
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
