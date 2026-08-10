package org.application.controller.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "BlockUserRequest", description = "Dados administrativos para bloquear uma conta.")
public record BlockUserRequest(
        @io.swagger.v3.oas.annotations.media.Schema(hidden = true) java.util.UUID administratorId,
        @NotBlank @Size(max = 2000) String reason
) {
    public BlockUserRequest(String reason) { this(null, reason); }
}
