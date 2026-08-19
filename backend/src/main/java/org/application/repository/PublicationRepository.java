package org.application.repository;

import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.application.model.PublicationVisibility;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicationRepository extends JpaRepository<Publication, UUID> {

    Optional<Publication> findByIdAndStatus(UUID id, PublicationStatus status);
    Optional<Publication> findByIdAndStatusAndVisibility(UUID id, PublicationStatus status, PublicationVisibility visibility);
    Page<Publication> findByStatusOrderByPublishedAtDescIdDesc(PublicationStatus status, Pageable pageable);
    Page<Publication> findByStatusAndVisibilityOrderByPublishedAtDescIdDesc(PublicationStatus status, PublicationVisibility visibility, Pageable pageable);
    Page<Publication> findByStatusAndTypeInOrderByPublishedAtDescIdDesc(PublicationStatus status, Collection<PublicationType> types, Pageable pageable);
    Page<Publication> findByStatusAndVisibilityAndTypeInOrderByPublishedAtDescIdDesc(PublicationStatus status, PublicationVisibility visibility, Collection<PublicationType> types, Pageable pageable);
    @Query(value = """
            select p from Publication p
            left join PublicationView v on v.publicationId = p.id and v.userId = :viewerId
            where p.status = :status and p.type in :types
            order by case when v.publicationId is null then 0 else 1 end, p.publishedAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from Publication p
            where p.status = :status and p.type in :types
            """)
    Page<Publication> findFeedForAuthenticatedViewerOrderByUnseenFirst(
            @Param("status") PublicationStatus status,
            @Param("types") Collection<PublicationType> types,
            @Param("viewerId") UUID viewerId,
            Pageable pageable);
    Page<Publication> findByStatusAndVisibilityAndTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(PublicationStatus status, PublicationVisibility visibility, String title, Pageable pageable);
    Page<Publication> findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(PublicationStatus status, String title, Pageable pageable);
    @Query("""
            select distinct p from Publication p
            left join Recipe r on r.publicationId = p.id and r.deletedAt is null
            left join RecipeIngredient i on i.recipeId = r.publicationId and i.deletedAt is null
            where p.status = :status and (:visibility is null or p.visibility = :visibility)
              and ((:title is not null and :title <> '' and lower(p.title) like lower(concat('%', :title, '%')))
                or (:ingredient is not null and :ingredient <> '' and lower(i.name) like lower(concat('%', :ingredient, '%'))))
            order by p.publishedAt desc, p.id desc
            """)
    Page<Publication> searchByTitleOrIngredient(
            @Param("status") PublicationStatus status,
            @Param("visibility") PublicationVisibility visibility,
            @Param("title") String title,
            @Param("ingredient") String ingredient,
            Pageable pageable);
    Page<Publication> findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(UUID authorId, PublicationStatus status, Pageable pageable);
    Page<Publication> findByAuthorIdAndStatusAndVisibilityOrderByPublishedAtDescIdDesc(UUID authorId, PublicationStatus status, PublicationVisibility visibility, Pageable pageable);
}
