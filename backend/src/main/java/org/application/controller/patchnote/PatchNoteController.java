package org.application.controller.patchnote;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.patchnote.response.PatchNoteResponse;
import org.application.service.PatchNoteService;
import org.application.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/patch-notes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "PatchNotes", description = "Notas de versão exibidas aos usuários após publicação.")
public class PatchNoteController {

    private final PatchNoteService patchNoteService;
    private final UserService userService;
    private final CurrentUser currentUser;

    @GetMapping("/unseen")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Notas de versão não vistas", description = "Lista as notas de versão publicadas desde a última vez que o usuário autenticado confirmou tê-las visto, da mais antiga para a mais recente.")
    @ApiResponse(responseCode = "200", description = "Notas retornadas.")
    public List<PatchNoteResponse> unseen(Authentication authentication) {
        var user = userService.findActive(currentUser.id(authentication));
        return patchNoteService.findUnseen(user).stream().map(PatchNoteResponse::of).toList();
    }
}
