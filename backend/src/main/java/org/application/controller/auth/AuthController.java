package org.application.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.controller.auth.request.ConfirmPasswordResetRequest;
import org.application.controller.auth.request.LoginRequest;
import org.application.controller.auth.request.RefreshTokenRequest;
import org.application.controller.auth.request.RequestPasswordResetRequest;
import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.auth.response.LoginResponse;
import org.application.controller.auth.response.CreatedUserResponse;
import org.application.controller.auth.response.UserInfoResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.config.CurrentUser;
import org.application.service.AuthService;
import org.application.service.UserService;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth", produces = "application/json")
@Tag(name = "Authentication", description = "Autenticação e emissão de tokens Bearer.")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final CurrentUser currentUser;

    @PostMapping("/register")
    @Operation(summary = "Criar conta", description = "Cria uma conta usando e-mail e senha (produto5.md v5 §5.1) — o @usuário é gerado automaticamente a partir do nome de exibição, editável depois.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
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
        String key = httpRequest.getRemoteAddr() + "|" + request.identifier().trim().toLowerCase(java.util.Locale.ROOT);
        return authService.login(request, key, httpRequest.getHeader("User-Agent"), clientIp(httpRequest));
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

    @PostMapping("/password-reset")
    @Operation(summary = "Pedir link de recuperação de senha", description = "Envia um link de recuperação por e-mail quando existe uma conta com o e-mail informado. Sempre responde com sucesso, exista ou não a conta, pra não revelar quem tem cadastro (docs/telas/11-recuperar-senha.html).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pedido registrado."),
            @ApiResponse(responseCode = "429", description = "Limite de pedidos excedido.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody RequestPasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirmar nova senha", description = "Troca a senha usando o token do link recebido por e-mail. O token vale por 1 hora e só pode ser usado uma vez; ao concluir, todas as sessões da conta são encerradas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha trocada."),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já usado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Informações básicas da sessão", description = "Retorna dados da conta autenticada que não vêm no token de login, como o nome de exibição (usado no menu da conta — docs/telas/04).")
    @ApiResponse(responseCode = "200", description = "Informações retornadas.")
    public UserInfoResponse info(Authentication authentication) {
        return UserInfoResponse.of(userService.findActive(currentUser.id(authentication)));
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Encerrar todas as sessões", description = "Revoga os refresh tokens de todos os dispositivos da conta autenticada.")
    @ApiResponse(responseCode = "204", description = "Todas as sessões foram encerradas.")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        authService.logoutAll(currentUser.id(authentication));
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
