package org.application.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.controller.auth.request.LoginRequest;
import org.application.controller.auth.request.RefreshTokenRequest;
import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.auth.response.LoginResponse;
import org.application.controller.auth.response.CreatedUserResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.service.AuthService;
import org.application.service.UserService;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth", produces = "application/json")
@Tag(name = "Authentication", description = "Autenticação e emissão de tokens Bearer.")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Criar conta", description = "Cria uma conta usando username e senha. O e-mail não participa do cadastro no MVP.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username já cadastrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CreatedUserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        CreatedUserResponse response = CreatedUserResponse.of(userService.create(request));
        return ResponseEntity.created(URI.create("/users/" + response.id())).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida as credenciais e emite um JWT para os endpoints protegidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido."),
            @ApiResponse(responseCode = "400", description = "Credenciais inválidas.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Limite de tentativas excedido.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String key = httpRequest.getRemoteAddr() + "|" + request.username().trim().toLowerCase(java.util.Locale.ROOT);
        return authService.login(request, key);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar sessão", description = "Revoga o refresh token atual e emite um novo par de tokens.")
    @ApiResponse(responseCode = "200", description = "Sessão renovada.")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Encerrar sessão", description = "Revoga o refresh token informado.")
    @ApiResponse(responseCode = "204", description = "Sessão encerrada.")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
