package org.application.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.application.controller.user.request.UpdateUserRequest;
import org.application.controller.user.request.BlockUserRequest;
import org.application.controller.user.request.ChangePasswordRequest;
import org.application.controller.user.request.UpdateNotificationPreferencesRequest;
import org.application.controller.collection.response.CollectionResponse;
import org.application.controller.user.response.NotificationPreferencesResponse;
import org.application.controller.user.response.UserResponse;
import org.application.controller.publication.response.PublicationResponse;
import org.application.controller.user.response.NotificationResponse;
import org.application.dto.PageResponse;
import org.application.service.PublicationService;
import org.application.service.PublicationResponseFactory;
import org.application.service.CollectionService;
import org.application.service.UserService;
import org.application.service.AccountSecurityService;
import org.application.service.FollowService;
import org.application.config.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import java.time.ZoneId;
import org.springframework.http.MediaType;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Operações de cadastro e gerenciamento de usuários.")
public class UserController {

    private final UserService userService;
    private final PublicationService publicationService;
    private final ZoneId applicationZoneId;
    private final AccountSecurityService accountSecurityService;
    private final FollowService followService;
    private final CollectionService collectionService;
    private final CurrentUser currentUser;
    private final PublicationResponseFactory responseFactory;

    @DeleteMapping("/{id}/account")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Anonimizar conta", description = "Exclui logicamente a conta e preserva as publicações anonimizadas.")
    @ApiResponse(responseCode = "204", description = "Conta anonimizada.")
    public ResponseEntity<Void> anonymize(@PathVariable UUID id, Authentication authentication) {
        ensureCurrentUser(id, authentication);
        accountSecurityService.anonymize(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Bloquear usuário", description = "Bloqueia e anonimiza uma conta, preservando HMAC do username.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário bloqueado."),
            @ApiResponse(responseCode = "400", description = "Operação não permitida."),
            @ApiResponse(responseCode = "403", description = "Apenas administradores podem bloquear usuários.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public ResponseEntity<Void> block(@PathVariable UUID id, @Valid @RequestBody BlockUserRequest request, Authentication authentication) {
        accountSecurityService.block(id, currentUser.id(authentication), request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getUserById", summary = "Consultar usuário", description = "Retorna os dados públicos de um usuário ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public UserResponse find(
            @Parameter(description = "UUID do usuário.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        var user = userService.findActive(id);
        return UserResponse.of(user, applicationZoneId, followService.countFollowers(id), followService.countFollowing(id),
                followedByCurrentUser(id, authentication));
    }

    @PutMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Seguir usuário", description = "Passa a seguir o usuário informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário seguido."),
            @ApiResponse(responseCode = "400", description = "Não é possível seguir a si mesmo.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> follow(
            @Parameter(description = "UUID do usuário a seguir.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        followService.follow(currentUser.id(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deixar de seguir usuário", description = "Remove logicamente o vínculo de seguir o usuário informado.")
    @ApiResponse(responseCode = "204", description = "Deixou de seguir.")
    public ResponseEntity<Void> unfollow(
            @Parameter(description = "UUID do usuário a deixar de seguir.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        followService.unfollow(currentUser.id(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/followers")
    @Operation(summary = "Listar seguidores", description = "Retorna os seguidores ativos do usuário, paginados.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
            @Parameter(name = "sort", description = "Direção da ordenação.", schema = @Schema(type = "string", example = "ASC"))
    })
    @ApiResponse(responseCode = "200", description = "Seguidores retornados.")
    public PageResponse<UserResponse> followers(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.of(followService.listFollowers(id, pageable), item -> summaryOf(item, authentication));
    }

    @GetMapping("/{id}/following")
    @Operation(summary = "Listar quem o usuário segue", description = "Retorna os perfis seguidos pelo usuário, paginados.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
            @Parameter(name = "sort", description = "Direção da ordenação.", schema = @Schema(type = "string", example = "ASC"))
    })
    @ApiResponse(responseCode = "200", description = "Perfis seguidos retornados.")
    public PageResponse<UserResponse> following(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.of(followService.listFollowing(id, pageable), item -> summaryOf(item, authentication));
    }

    private UserResponse summaryOf(org.application.model.User item, Authentication authentication) {
        return UserResponse.of(item, applicationZoneId, followService.countFollowers(item.getId()),
                followService.countFollowing(item.getId()), followedByCurrentUser(item.getId(), authentication));
    }

    private Boolean followedByCurrentUser(UUID profileId, Authentication authentication) {
        UUID viewerId = authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? null : currentUser.id(authentication);
        if (viewerId == null || viewerId.equals(profileId)) {
            return null;
        }
        return followService.isFollowing(viewerId, profileId);
    }

    @GetMapping("/{id}/publications")
    @Operation(operationId = "getUserPublications", summary = "Listar publicações do perfil", description = "Retorna publicações ativas do perfil, paginadas. Visitantes recebem somente PUBLIC; usuários autenticados podem receber PUBLIC e INTERNAL.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
            @Parameter(name = "sort", description = "Direção da ordenação.", schema = @Schema(type = "string", example = "ASC"))
    })
    @ApiResponse(responseCode = "200", description = "Publicações retornadas.")
    public PageResponse<PublicationResponse> publications(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        UUID viewerId = authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? null : currentUser.id(authentication);
        return PageResponse.of(publicationService.profile(id, pageable, viewerId), item -> responseFactory.of(item, applicationZoneId, viewerId));
    }

    @GetMapping("/{id}/collections")
    @Operation(operationId = "getUserCollections", summary = "Listar coleções do perfil", description = "Retorna as coleções do perfil, paginadas. Visitantes e outros usuários recebem apenas coleções públicas; o próprio autor recebe todas.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20"))
    })
    @ApiResponse(responseCode = "200", description = "Coleções retornadas.")
    public PageResponse<CollectionResponse> collections(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        UUID viewerId = authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? null : currentUser.id(authentication);
        var author = userService.findActive(id);
        return PageResponse.of(collectionService.listByAuthor(id, viewerId, pageable), item -> CollectionResponse.of(
                item, applicationZoneId, author, collectionService.countPublications(item.getId()),
                collectionService.countFollowers(item.getId()),
                viewerId == null || viewerId.equals(item.getAuthorId()) ? null : collectionService.isFollowing(viewerId, item.getId())));
    }

    @GetMapping("/{id}/notifications")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar notificações", description = "Retorna notificações privadas paginadas do usuário.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
            @Parameter(name = "sort", description = "Direção da ordenação.", schema = @Schema(type = "string", example = "ASC"))
    })
    @ApiResponse(responseCode = "200", description = "Notificações retornadas.")
    public PageResponse<NotificationResponse> notifications(
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        return PageResponse.of(userService.notifications(id, pageable), NotificationResponse::of);
    }

    @GetMapping("/{id}/notification-preferences")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar preferências de notificação", description = "Retorna as preferências de notificação da conta autenticada.")
    @ApiResponse(responseCode = "200", description = "Preferências retornadas.")
    public NotificationPreferencesResponse notificationPreferences(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        return NotificationPreferencesResponse.of(userService.findActive(id));
    }

    @PatchMapping("/{id}/notification-preferences")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualizar preferências de notificação", description = "Atualiza as preferências de notificação da conta autenticada.")
    @ApiResponse(responseCode = "204", description = "Preferências atualizadas.")
    public ResponseEntity<Void> updateNotificationPreferences(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        userService.updateNotifyOnFollowedPublish(id, request.notifyOnFollowedPublish());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "updateCurrentUser", summary = "Atualizar usuário", description = "Atualiza o username e/ou o nome exibido do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou nenhum campo informado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username já cadastrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public UserResponse update(
            @Parameter(description = "UUID do usuário.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        var user = userService.update(id, request);
        return UserResponse.of(user, applicationZoneId, followService.countFollowers(id), followService.countFollowing(id), null);
    }

    @PatchMapping("/{id}/onboarding")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Concluir onboarding", description = "Marca o onboarding do primeiro login como concluído para a conta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Onboarding concluído."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> completeOnboarding(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        userService.completeOnboarding(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/patch-notes/seen")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Confirmar leitura das notas de versão", description = "Marca todas as notas de versão publicadas até agora como vistas pela conta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notas marcadas como vistas."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> markPatchNotesSeen(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        userService.markPatchNotesSeen(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Alterar senha", description = "Altera a senha da conta autenticada mediante confirmação da senha atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada."),
            @ApiResponse(responseCode = "400", description = "Senha atual inválida ou dados inválidos.")
    })
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "deleteCurrentUser", summary = "Excluir usuário", description = "Realiza a exclusão lógica de um usuário ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID do usuário.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        ensureCurrentUser(id, authentication);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void ensureCurrentUser(UUID userId, Authentication authentication) {
        if (!userId.equals(currentUser.id(authentication, userId))) {
            throw new org.application.service.exception.InvalidOperationException("A conta autenticada não corresponde ao usuário informado.");
        }
    }
}

