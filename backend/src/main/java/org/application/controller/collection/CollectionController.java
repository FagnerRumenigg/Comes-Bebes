package org.application.controller.collection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.collection.request.CreateCollectionRequest;
import org.application.controller.collection.request.InviteCollectionMemberRequest;
import org.application.controller.collection.request.UpdateCollectionRequest;
import org.application.controller.collection.response.CollectionInviteResponse;
import org.application.controller.collection.response.CollectionResponse;
import org.application.controller.publication.response.PublicationResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.controller.user.response.UserResponse;
import org.application.dto.PageResponse;
import org.application.model.CollectionVisibility;
import org.application.model.PublicationCollection;
import org.application.model.User;
import org.application.repository.UserRepository;
import org.application.service.CollectionService;
import org.application.service.PublicationResponseFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/collections", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Collections", description = "Coleções curadas de publicações.")
public class CollectionController {

    private final CollectionService collectionService;
    private final PublicationResponseFactory publicationResponseFactory;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final ZoneId applicationZoneId;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "createCollection", summary = "Criar coleção", description = "Cria uma nova coleção curada de publicações.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coleção criada."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CollectionResponse> create(
            @Valid @RequestBody CreateCollectionRequest request,
            Authentication authentication
    ) {
        UUID authorId = currentUser.id(authentication);
        PublicationCollection collection = collectionService.create(authorId, request.name(), request.description(),
                CollectionVisibility.valueOf(request.visibility()));
        CollectionResponse response = toResponse(collection, authorId);
        return ResponseEntity.created(URI.create("/collections/" + response.id())).body(response);
    }

    @GetMapping("/followed")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "getFollowedCollections", summary = "Listar coleções seguidas", description = "Retorna as coleções que a conta autenticada segue, paginadas.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20"))
    })
    @ApiResponse(responseCode = "200", description = "Coleções retornadas.")
    public PageResponse<CollectionResponse> followed(
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        UUID viewerId = currentUser.id(authentication);
        return PageResponse.of(collectionService.listFollowed(viewerId, pageable), item -> toResponse(item, viewerId));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getCollectionById", summary = "Consultar coleção", description = "Retorna uma coleção. Coleções privadas só ficam visíveis para o próprio autor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coleção encontrada."),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CollectionResponse find(
            @Parameter(description = "UUID da coleção.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID viewerId = viewerId(authentication);
        return toResponse(collectionService.findAccessible(id, viewerId), viewerId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "updateCollection", summary = "Editar coleção", description = "Edita nome, descrição e visibilidade de uma coleção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coleção atualizada."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CollectionResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCollectionRequest request,
            Authentication authentication
    ) {
        UUID authorId = currentUser.id(authentication);
        PublicationCollection collection = collectionService.update(id, authorId, request.name(), request.description(),
                CollectionVisibility.valueOf(request.visibility()));
        return toResponse(collection, authorId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "deleteCollection", summary = "Excluir coleção", description = "Realiza exclusão lógica de uma coleção pelo autor.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Coleção excluída."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        collectionService.remove(id, currentUser.id(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/publications")
    @Operation(operationId = "getCollectionPublications", summary = "Listar publicações da coleção", description = "Retorna as publicações da coleção, na ordem em que foram adicionadas, paginadas.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicações retornadas."),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PageResponse<PublicationResponse> publications(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        UUID viewerId = viewerId(authentication);
        return PageResponse.of(collectionService.listPublications(id, viewerId, pageable),
                item -> publicationResponseFactory.of(item, applicationZoneId, viewerId));
    }

    @PutMapping("/{id}/publications/{publicationId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "addCollectionPublication", summary = "Adicionar publicação à coleção", description = "Adiciona a publicação ao final da coleção. Idempotente: já presente é ignorado silenciosamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicação adicionada."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção ou publicação não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> addPublication(
            @PathVariable UUID id,
            @PathVariable UUID publicationId,
            Authentication authentication
    ) {
        collectionService.addPublication(id, currentUser.id(authentication), publicationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/publications/{publicationId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "removeCollectionPublication", summary = "Remover publicação da coleção", description = "Remove a publicação da coleção. Idempotente: já ausente é ignorado silenciosamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicação removida."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> removePublication(
            @PathVariable UUID id,
            @PathVariable UUID publicationId,
            Authentication authentication
    ) {
        collectionService.removePublication(id, currentUser.id(authentication), publicationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "followCollection", summary = "Seguir coleção", description = "Passa a seguir a coleção informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Coleção seguida."),
            @ApiResponse(responseCode = "400", description = "Não é possível seguir a própria coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> follow(@PathVariable UUID id, Authentication authentication) {
        collectionService.follow(currentUser.id(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "unfollowCollection", summary = "Deixar de seguir coleção", description = "Remove logicamente o vínculo de seguir a coleção informada.")
    @ApiResponse(responseCode = "204", description = "Deixou de seguir.")
    public ResponseEntity<Void> unfollow(@PathVariable UUID id, Authentication authentication) {
        collectionService.unfollow(currentUser.id(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invite-link")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "getCollectionInviteLink", summary = "Consultar link de convite", description = "Retorna o token do link de convite ativo da coleção, criando um na primeira chamada. Só o autor pode consultar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token retornado."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CollectionInviteResponse inviteLink(@PathVariable UUID id, Authentication authentication) {
        return new CollectionInviteResponse(collectionService.getOrCreateInviteLink(id, currentUser.id(authentication)));
    }

    @PostMapping("/{id}/invite-link")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "regenerateCollectionInviteLink", summary = "Gerar novo link de convite", description = "Revoga o link de convite atual e cria outro no lugar — quem tinha o link antigo perde o acesso a partir de agora. Só o autor pode gerar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo token retornado."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CollectionInviteResponse regenerateInviteLink(@PathVariable UUID id, Authentication authentication) {
        return new CollectionInviteResponse(collectionService.regenerateInviteLink(id, currentUser.id(authentication)));
    }

    @PostMapping("/invites/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "acceptCollectionInvite", summary = "Aceitar convite de coleção", description = "Passa a ver a coleção 'Para quem eu escolher' associada ao token, como quem segue uma coleção pública.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite aceito; coleção retornada."),
            @ApiResponse(responseCode = "404", description = "Convite inválido ou coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CollectionResponse acceptInvite(@PathVariable String token, Authentication authentication) {
        UUID viewerId = currentUser.id(authentication);
        return toResponse(collectionService.acceptInvite(token, viewerId), viewerId);
    }

    @GetMapping("/{id}/invitees")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "getCollectionInvitees", summary = "Listar quem já tem acesso", description = "Retorna as pessoas que já aceitaram o convite da coleção. Só o autor pode consultar.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoas retornadas."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PageResponse<UserResponse> invitees(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        UUID authorId = currentUser.id(authentication);
        return PageResponse.of(collectionService.listInvitees(id, authorId, pageable),
                user -> UserResponse.of(user, applicationZoneId, null));
    }

    @PutMapping("/{id}/invitees")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "inviteCollectionMember", summary = "Convidar pessoa por @usuário", description = "Concede acesso imediato a uma coleção \"Para quem eu escolher\", sem passo de aceite. Só o autor pode convidar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoa convidada."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor, a coleção não é \"Para quem eu escolher\", ou você tentou convidar a si mesmo.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção ou usuário não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public UserResponse inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteCollectionMemberRequest request,
            Authentication authentication
    ) {
        UUID authorId = currentUser.id(authentication);
        User invitee = collectionService.inviteByUsername(id, authorId, request.username());
        return UserResponse.of(invitee, applicationZoneId, null);
    }

    @DeleteMapping("/{id}/invitees/{userId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "removeCollectionInvitee", summary = "Remover acesso de uma pessoa", description = "Tira o acesso de uma pessoa específica, sem afetar as outras nem revogar o link de convite. Só o autor pode remover.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Acesso removido, ou já inexistente."),
            @ApiResponse(responseCode = "400", description = "Você não é o autor desta coleção.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coleção não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> removeInvitee(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        collectionService.removeInvitee(id, currentUser.id(authentication), userId);
        return ResponseEntity.noContent().build();
    }

    private CollectionResponse toResponse(PublicationCollection collection, UUID viewerId) {
        var author = userRepository.findById(collection.getAuthorId()).orElse(null);
        long publicationsCount = collectionService.countPublications(collection.getId());
        var coverImageUrls = collectionService.coverImageUrls(collection.getId());
        boolean isOwner = viewerId != null && viewerId.equals(collection.getAuthorId());
        // Contador de seguidores só para o dono: visitante nunca vê (produto5.md v5 §3.1, impl10.md v10 §13.8).
        Long followersCount = isOwner ? collectionService.countFollowers(collection.getId()) : null;
        Boolean followedByCurrentUser = isOwner ? null : viewerId == null ? null : collectionService.isFollowing(viewerId, collection.getId());
        return CollectionResponse.of(collection, applicationZoneId, author, publicationsCount, coverImageUrls, followersCount, followedByCurrentUser);
    }

    private UUID viewerId(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? null : currentUser.id(authentication);
    }
}
