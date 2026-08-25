package org.application.controller.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.application.controller.document.response.DocumentResponse;
import org.application.service.ContentDocumentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/documents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Documents", description = "Termos de Serviço, Política de Privacidade e FAQ.")
public class DocumentController {

    private final ContentDocumentService contentDocumentService;
    private final ZoneId applicationZoneId;

    @GetMapping("/{slug}")
    @Operation(summary = "Consultar documento", description = "Retorna um documento público pelo slug (ex.: TERMS_OF_SERVICE, PRIVACY_POLICY, FAQ).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento retornado."),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado.", content = @Content(schema = @Schema(implementation = org.application.controller.response.ApiErrorResponse.class)))
    })
    public DocumentResponse bySlug(@PathVariable String slug) {
        return DocumentResponse.of(contentDocumentService.findBySlug(slug), applicationZoneId);
    }
}
