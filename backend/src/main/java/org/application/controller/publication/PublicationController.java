package org.application.controller.publication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.application.controller.publication.request.CreatePublicationRequest;
import org.application.controller.publication.request.UpdatePublicationRequest;
import org.application.controller.publication.request.ReactionRequest;
import org.application.controller.publication.request.SavePublicationRequest;
import org.application.controller.publication.request.CreateMyVersionRequest;
import org.application.controller.publication.request.CreateMyVersionUploadRequest;
import org.application.controller.publication.request.CreateReportRequest;
import org.application.controller.publication.request.CreatePublicationUploadRequest;
import org.application.controller.publication.request.DeletePublicationRequest;
import org.application.service.ReportService;
import org.application.service.ReactionService;
import org.application.service.SavedPublicationService;
import org.application.controller.publication.response.PublicationResponse;
import org.application.controller.publication.response.RecipeResponse;
import org.application.controller.publication.response.SavedPublicationResponse;
import org.application.dto.PageResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.service.PublicationService;
import org.application.service.PublicationResponseFactory;
import org.application.service.storage.ImageStorage;
import org.application.service.RecipeService;
import org.application.config.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.application.model.PublicationType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/publications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Publications", description = "Consulta de publicações ativas.")
public class PublicationController {

    private final PublicationService publicationService;
    private final RecipeService recipeService;
    private final ReactionService reactionService;
    private final SavedPublicationService savedPublicationService;
    private final ReportService reportService;
    private final PublicationResponseFactory responseFactory;
    private final ImageStorage imageStorage;

    @GetMapping("/feed")
    @Operation(summary = "Listar feed", description = "Retorna publicações ativas paginadas. Visitantes recebem somente PUBLIC; usuários autenticados podem receber PUBLIC e INTERNAL. A primeira página é 1. Sem o parâmetro types, retorna todos os tipos (Mix).")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
    })
    @ApiResponse(responseCode = "200", description = "Feed retornado.")
    public PageResponse<PublicationResponse> feed(
            @Parameter(hidden = true) Pageable pageable,
            @Parameter(description = "Filtra por um ou mais tipos de publicação. Sem valor, retorna todos (Mix).")
            @RequestParam(required = false) Set<PublicationType> types,
            Authentication authentication
    ) {
        return PageResponse.of(publicationService.feed(pageable, viewerId(authentication), types), item -> responseFactory.of(item, applicationZoneId, viewerId(authentication)));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar publicações", description = "Busca publicações ativas por título ou ingrediente, com paginação. Visitantes recebem somente PUBLIC; usuários autenticados podem receber PUBLIC e INTERNAL.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
            @Parameter(name = "sort", description = "Direção da ordenação.", schema = @Schema(type = "string", example = "ASC"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados retornados."),
            @ApiResponse(responseCode = "400", description = "Informe título ou ingrediente para pesquisar.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PageResponse<PublicationResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String ingredient,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.of(publicationService.search(title, ingredient, pageable, viewerId(authentication)), item -> responseFactory.of(item, applicationZoneId, viewerId(authentication)));
    }
    private final ZoneId applicationZoneId;
    private final CurrentUser currentUser;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Criar publicação com upload", description = "Cria uma publicação recebendo a imagem como multipart. Antes do armazenamento, o backend valida sincronicamente se o arquivo é uma imagem válida e se apresenta comida.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicação criada."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, imagem inválida ou imagem rejeitada pelo classificador de comida.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "A imagem excede o limite de 20 MB.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Limite de publicações no período foi excedido.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "O validador de imagens está indisponível.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PublicationResponse> createUpload(
            @Valid @RequestPart("data") CreatePublicationUploadRequest request,
            @RequestPart("image") MultipartFile image,
            Authentication authentication
    ) throws java.io.IOException {
        if (image.isEmpty()) {
            throw new org.application.service.exception.InvalidOperationException("A imagem é obrigatória.");
        }
        UUID authorId = currentUser.id(authentication);
        PublicationResponse response = responseFactory.of(
                publicationService.createUpload(request, authorId, image.getBytes(), image.getOriginalFilename(), image.getContentType()),
                applicationZoneId, authorId);
        return ResponseEntity.created(URI.create("/publications/" + response.id())).body(response);
    }

    @Hidden
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PublicationResponse> createLegacy(
            @Valid @org.springframework.web.bind.annotation.RequestBody CreatePublicationRequest request,
            Authentication authentication
    ) {
        UUID authorId = currentUser.id(authentication, request.authorId());
        PublicationResponse response = responseFactory.of(publicationService.create(request, authorId), applicationZoneId, authorId);
        return ResponseEntity.created(URI.create("/publications/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPublicationById", summary = "Consultar publicação", description = "Retorna uma publicação ativa e seus dados textuais. Visitantes acessam somente PUBLIC; usuários autenticados também podem acessar INTERNAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicação encontrada."),
            @ApiResponse(responseCode = "404", description = "Publicação não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PublicationResponse find(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return responseFactory.of(publicationService.findAccessible(id, viewerId(authentication)), applicationZoneId, viewerId(authentication));
    }

    @GetMapping("/saved")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar publicações salvas", description = "Retorna a lista privada e paginada do usuário informado.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20")),
            @Parameter(name = "sort", description = "Direção da ordenação.", schema = @Schema(type = "string", example = "ASC"))
    })
    @ApiResponse(responseCode = "200", description = "Lista retornada.")
    public PageResponse<SavedPublicationResponse> saved(
            @Parameter(hidden = true) Pageable pageable
            , Authentication authentication
    ) {
        return PageResponse.of(savedPublicationService.list(currentUser.id(authentication), pageable), item -> SavedPublicationResponse.of(item));
    }

    @Hidden
    @GetMapping("/saved/{userId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public PageResponse<SavedPublicationResponse> savedLegacy(
            @PathVariable UUID userId,
            @Parameter(hidden = true) Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.of(savedPublicationService.list(currentUser.id(authentication, userId), pageable), SavedPublicationResponse::of);
    }

    @GetMapping("/{id}/recipe")
    @Operation(operationId = "getPublicationRecipe", summary = "Consultar receita", description = "Retorna a receita ativa e os ingredientes ordenados da publicação. Visitantes acessam somente PUBLIC; usuários autenticados também podem acessar INTERNAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receita encontrada."),
            @ApiResponse(responseCode = "404", description = "Receita não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public RecipeResponse findRecipe(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
            , Authentication authentication
    ) {
        publicationService.findAccessible(id, viewerId(authentication));
        return RecipeResponse.of(recipeService.findDetails(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "updatePublication", summary = "Editar publicação", description = "Edita dados textuais ou converte uma publicação entre DISH e RECIPE. A imagem permanece imutável.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicação atualizada."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou conversão inconsistente.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Publicação ou autor não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PublicationResponse update(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdatePublicationRequest request,
            Authentication authentication
    ) {
        UUID authorId = currentUser.id(authentication);
        return responseFactory.of(publicationService.update(id, request, authorId), applicationZoneId, authorId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "deletePublication", summary = "Excluir publicação", description = "Realiza exclusão lógica da publicação pelo autor. A imagem não é removida nesta etapa simulada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicação excluída."),
            @ApiResponse(responseCode = "400", description = "Usuário não é o autor.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Publicação não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        publicationService.remove(id, currentUser.id(authentication));
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Aplicar reação", description = "Aplica ou reativa uma reação do usuário. O mesmo usuário pode aplicar tipos diferentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reação aplicada."),
            @ApiResponse(responseCode = "400", description = "Usuário não pode reagir à própria publicação.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Publicação, usuário ou reação não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<Void> applyReaction(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody ReactionRequest request,
            Authentication authentication
    ) {
        reactionService.apply(id, request, currentUser.id(authentication));
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/reactions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remover reação", description = "Remove logicamente a reação do usuário, sem apagar o histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reação removida ou já inexistente."),
            @ApiResponse(responseCode = "404", description = "Publicação, usuário ou reação não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<Void> removeReaction(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody ReactionRequest request,
            Authentication authentication
    ) {
        reactionService.remove(id, request, currentUser.id(authentication));
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/saved")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Salvar publicação", description = "Salva ou reativa uma publicação na lista privada do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicação salva."),
            @ApiResponse(responseCode = "404", description = "Publicação ou usuário não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<Void> save(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody SavePublicationRequest request,
            Authentication authentication
    ) {
        savedPublicationService.saveAs(id, currentUser.id(authentication));
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/saved")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remover publicação salva", description = "Remove logicamente uma publicação da lista privada do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicação removida da lista."),
            @ApiResponse(responseCode = "404", description = "Publicação ou usuário não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<Void> removeSaved(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody SavePublicationRequest request,
            Authentication authentication
    ) {
        savedPublicationService.removeAs(id, currentUser.id(authentication));
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/my-versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Criar minha versão", description = "Cria uma receita própria inspirada na publicação original. A imagem também passa pela validação síncrona de formato e conteúdo antes do armazenamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Minha versão criada."),
            @ApiResponse(responseCode = "400", description = "Receita, dados da versão ou imagem inválidos; também pode indicar rejeição pelo classificador de comida.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Publicação original, receita ou autor não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "A imagem excede o limite de 20 MB.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Limite de publicações no período foi excedido.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "O validador de imagens está indisponível.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<PublicationResponse> createMyVersion(
            @Parameter(description = "UUID da publicação original.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestPart("data") CreateMyVersionUploadRequest request,
            @RequestPart("image") MultipartFile image,
            Authentication authentication
    ) throws java.io.IOException {
        UUID authorId = currentUser.id(authentication);
        if (image.isEmpty()) {
            throw new org.application.service.exception.InvalidOperationException("A imagem é obrigatória.");
        }
        CreateMyVersionRequest domainRequest = new CreateMyVersionRequest(
                authorId, request.visibility(), request.titleSuffix(), request.changeSummary(),
                "file://local/upload", request.recipe());
        PublicationResponse response = responseFactory.of(publicationService.createMyVersionUpload(
                id, domainRequest, authorId, image.getBytes(), image.getOriginalFilename(), image.getContentType()), applicationZoneId, authorId);
        return org.springframework.http.ResponseEntity.created(java.net.URI.create("/publications/" + response.id())).body(response);
    }

    @PostMapping("/{id}/reports")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Denunciar publicação", description = "Registra uma denúncia estruturada. Ao atingir o limite configurado, abre um caso de moderação.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Denúncia registrada."),
            @ApiResponse(responseCode = "400", description = "O autor não pode denunciar a própria publicação.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Publicação, usuário ou motivo não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Usuário já denunciou esta publicação.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public org.springframework.http.ResponseEntity<Void> report(
            @Parameter(description = "UUID da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateReportRequest request,
            Authentication authentication
    ) {
        reportService.create(id, request, currentUser.id(authentication));
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    private UUID viewerId(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? null : currentUser.id(authentication);
    }
}

