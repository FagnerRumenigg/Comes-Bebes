package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateReportRequest", description = "Dados para denunciar uma publicação.")
public record CreateReportRequest(
        @Schema(hidden = true) java.util.UUID reporterId,
        @Schema(description = "Código do motivo estruturado.", example = "NOT_FOOD")
        @NotBlank String reasonCode,
        @Schema(description = "Descrição opcional da denúncia.", example = "A publicação não apresenta comida.", nullable = true)
        @Size(max = 1000) String description
) {
    public CreateReportRequest(String reasonCode, String description) { this(null, reasonCode, description); }
}
