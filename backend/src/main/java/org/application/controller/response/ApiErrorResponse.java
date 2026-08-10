package org.application.controller.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

@Builder
@Schema(name = "ApiErrorResponse", description = "Formato padrão de erro da API.")
public record ApiErrorResponse(
        @Schema(description = "Data e hora do erro.", example = "2026-08-08T12:00:00Z")
        OffsetDateTime timestamp,
        @Schema(description = "Código HTTP retornado.", example = "404")
        int status,
        @Schema(description = "Descrição do status HTTP.", example = "Not Found")
        String error,
        @Schema(description = "Código estável para tratamento pelo frontend.", example = "USER_NOT_FOUND")
        String code,
        @Schema(description = "Mensagem explicando o erro.", example = "Usuário não encontrado.")
        String message,
        @Schema(description = "Erros por campo para validações de formulários.", nullable = true)
        Map<String, String> fieldErrors
) {
}
