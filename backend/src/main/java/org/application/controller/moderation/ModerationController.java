package org.application.controller.moderation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.controller.moderation.request.DecideModerationCaseRequest;
import org.application.controller.moderation.response.ModerationCaseResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.service.ModerationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.application.config.CurrentUser;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/moderation/cases", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Moderation", description = "Fila e decisões administrativas de moderação.")
public class ModerationController {

    private final ModerationService moderationService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar casos pendentes", description = "Retorna os casos pendentes em ordem de abertura.")
    @ApiResponse(responseCode = "200", description = "Casos pendentes retornados.")
    public List<ModerationCaseResponse> pending() {
        return moderationService.pendingCases().stream().map(ModerationCaseResponse::of).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "getModerationCaseById", summary = "Consultar caso", description = "Retorna um caso de moderação específico para análise administrativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caso encontrado."),
            @ApiResponse(responseCode = "404", description = "Caso não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ModerationCaseResponse find(
            @Parameter(description = "UUID do caso.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {
        return ModerationCaseResponse.of(moderationService.find(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Decidir caso", description = "Aprova, oculta ou remove a publicação e resolve as denúncias associadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caso decidido."),
            @ApiResponse(responseCode = "400", description = "Decisão inválida ou sem justificativa.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Caso não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ModerationCaseResponse decide(
            @Parameter(description = "UUID do caso.", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody DecideModerationCaseRequest request,
            Authentication authentication
    ) {
        return ModerationCaseResponse.of(moderationService.decide(id, request,
                currentUser.id(authentication, authentication == null ? request.reviewerId() : null)));
    }
}

