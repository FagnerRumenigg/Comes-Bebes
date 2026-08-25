package org.application.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.response.ApiErrorResponse;
import org.application.controller.user.response.UserResponse;
import org.application.service.FollowService;
import org.application.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/u", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Consulta de perfis por username.")
public class ProfileController {
    private final UserService userService;
    private final FollowService followService;
    private final CurrentUser currentUser;
    private final ZoneId applicationZoneId;

    @GetMapping("/{username}")
    @Operation(summary = "Consultar perfil por username", description = "Retorna o perfil público pelo username.")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado.")
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public UserResponse findByUsername(
            @Parameter(description = "Username público.", example = "fagner")
            @PathVariable String username,
            Authentication authentication
    ) {
        var user = userService.findActiveByUsername(username);
        UUID viewerId = authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? null : currentUser.id(authentication);
        Boolean followedByCurrentUser = viewerId == null || viewerId.equals(user.getId())
                ? null : followService.isFollowing(viewerId, user.getId());
        return UserResponse.of(user, applicationZoneId, followedByCurrentUser);
    }
}
