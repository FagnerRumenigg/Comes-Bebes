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
    // "Só para mim" nunca aparece no feed nem na busca — nem para o próprio autor
    // (impl10.md v10 §17: "Aparece no feed: Não", sem ressalva). O autor continua
    // enxergando a própria publicação PRIVATE, só que pelo perfil, não por aqui.
    @Query(value = """
            select p from Publication p
            left join PublicationView v on v.publicationId = p.id and v.userId = :viewerId
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
            order by case when v.publicationId is null then 0 else 1 end, p.publishedAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
            """)
    Page<Publication> findFeedForAuthenticatedViewerOrderByUnseenFirst(
            @Param("status") PublicationStatus status,
            @Param("types") Collection<PublicationType> types,
            @Param("viewerId") UUID viewerId,
            Pageable pageable);

    // "Ordenar por: Mais antigas" — cronológico puro, sem o agrupamento de
    // não-vistas (docs/telas/05-feed.html, facet "Ordenar por").
    Page<Publication> findByStatusAndVisibilityAndTypeInOrderByPublishedAtAscIdAsc(PublicationStatus status, PublicationVisibility visibility, Collection<PublicationType> types, Pageable pageable);
    Page<Publication> findByStatusAndVisibilityNotAndTypeInOrderByPublishedAtAscIdAsc(PublicationStatus status, PublicationVisibility visibility, Collection<PublicationType> types, Pageable pageable);

    // "De quem: Quem eu sigo" — mesma regra de visibilidade e agrupamento de
    // não-vistas do feed geral, restrita aos autores seguidos pelo viewer.
    @Query(value = """
            select p from Publication p
            left join PublicationView v on v.publicationId = p.id and v.userId = :viewerId
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.authorId in (select f.followedId from Follow f where f.followerId = :viewerId and f.deletedAt is null)
            order by case when v.publicationId is null then 0 else 1 end, p.publishedAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.authorId in (select f.followedId from Follow f where f.followerId = :viewerId and f.deletedAt is null)
            """)
    Page<Publication> findFeedByFollowedAuthorsOrderByUnseenFirst(
            @Param("status") PublicationStatus status,
            @Param("types") Collection<PublicationType> types,
            @Param("viewerId") UUID viewerId,
            Pageable pageable);

    @Query(value = """
            select p from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.authorId in (select f.followedId from Follow f where f.followerId = :viewerId and f.deletedAt is null)
            order by p.publishedAt asc, p.id asc
            """,
            countQuery = """
            select count(p) from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.authorId in (select f.followedId from Follow f where f.followerId = :viewerId and f.deletedAt is null)
            """)
    Page<Publication> findFeedByFollowedAuthorsOrderByPublishedAtAsc(
            @Param("status") PublicationStatus status,
            @Param("types") Collection<PublicationType> types,
            @Param("viewerId") UUID viewerId,
            Pageable pageable);

    // "De quem: Minhas coleções" — publicações que estão em alguma coleção
    // seguida pelo viewer (CollectionFollow), não as coleções de autoria dele.
    @Query(value = """
            select p from Publication p
            left join PublicationView v on v.publicationId = p.id and v.userId = :viewerId
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.id in (select cp.publicationId from CollectionPublication cp
                           where cp.collectionId in (select cf.collectionId from CollectionFollow cf where cf.followerId = :viewerId and cf.deletedAt is null))
            order by case when v.publicationId is null then 0 else 1 end, p.publishedAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.id in (select cp.publicationId from CollectionPublication cp
                           where cp.collectionId in (select cf.collectionId from CollectionFollow cf where cf.followerId = :viewerId and cf.deletedAt is null))
            """)
    Page<Publication> findFeedByFollowedCollectionsOrderByUnseenFirst(
            @Param("status") PublicationStatus status,
            @Param("types") Collection<PublicationType> types,
            @Param("viewerId") UUID viewerId,
            Pageable pageable);

    @Query(value = """
            select p from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.id in (select cp.publicationId from CollectionPublication cp
                           where cp.collectionId in (select cf.collectionId from CollectionFollow cf where cf.followerId = :viewerId and cf.deletedAt is null))
            order by p.publishedAt asc, p.id asc
            """,
            countQuery = """
            select count(p) from Publication p
            where p.status = :status and p.type in :types
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and p.id in (select cp.publicationId from CollectionPublication cp
                           where cp.collectionId in (select cf.collectionId from CollectionFollow cf where cf.followerId = :viewerId and cf.deletedAt is null))
            """)
    Page<Publication> findFeedByFollowedCollectionsOrderByPublishedAtAsc(
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
    // "Só para mim" nunca aparece na busca, nem para o próprio autor — mesma regra do feed.
    @Query(value = """
            select distinct p from Publication p
            left join Recipe r on r.publicationId = p.id and r.deletedAt is null
            left join RecipeIngredient i on i.recipeId = r.publicationId and i.deletedAt is null
            where p.status = :status
              and p.visibility <> org.application.model.PublicationVisibility.PRIVATE
              and ((:title is not null and :title <> '' and lower(p.title) like lower(concat('%', :title, '%')))
                or (:ingredient is not null and :ingredient <> '' and lower(i.name) like lower(concat('%', :ingredient, '%'))))
            order by p.publishedAt desc, p.id desc
            """)
    Page<Publication> searchByTitleOrIngredientForAuthenticatedViewer(
            @Param("status") PublicationStatus status,
            @Param("title") String title,
            @Param("ingredient") String ingredient,
            Pageable pageable);
    Page<Publication> findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(UUID authorId, PublicationStatus status, Pageable pageable);
    Page<Publication> findByAuthorIdAndStatusAndVisibilityOrderByPublishedAtDescIdDesc(UUID authorId, PublicationStatus status, PublicationVisibility visibility, Pageable pageable);
    Page<Publication> findByAuthorIdAndStatusAndVisibilityInOrderByPublishedAtDescIdDesc(UUID authorId, PublicationStatus status, Collection<PublicationVisibility> visibilities, Pageable pageable);
}
