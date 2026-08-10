package org.application.controller.moderation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Schema(name = "DecideModerationCaseRequest", description = "Decisão administrativa de um caso de moderação.")
public record DecideModerationCaseRequest(
        @io.swagger.v3.oas.annotations.media.Schema(hidden = true) java.util.UUID reviewerId,
        @Schema(description = "Decisão do caso.", example = "KEPT", allowableValues = {"KEPT", "HIDDEN", "REMOVED"})
        @NotBlank String decision,
        @Schema(description = "Justificativa obrigatória para HIDDEN e REMOVED.", example = "Conteúdo viola as regras do MVP.", nullable = true)
        @Size(max = 2000) String decisionNote
) {
    public DecideModerationCaseRequest(String decision, String decisionNote) { this(null, decision, decisionNote); }
}
