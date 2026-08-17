package org.application.controller.biometric;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.auth.response.LoginResponse;
import org.application.controller.biometric.request.CompleteBiometricAuthenticationRequest;
import org.application.controller.biometric.request.CompleteBiometricRegistrationRequest;
import org.application.controller.biometric.request.StartBiometricAuthenticationRequest;
import org.application.controller.biometric.request.StartBiometricRegistrationRequest;
import org.application.controller.biometric.response.BiometricAuthenticationStartResponse;
import org.application.controller.biometric.response.BiometricRegistrationStartResponse;
import org.application.controller.biometric.response.BiometricResponse;
import org.application.controller.biometric.response.BiometricStatusResponse;
import org.application.controller.response.ApiErrorResponse;
import org.application.repository.UserRepository;
import org.application.service.BiometricService;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth/biometric", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Biometric", description = "Login por biometria (Face ID / Fingerprint / Windows Hello) via WebAuthn (IDEIA-020).")
public class BiometricController {

    private final BiometricService biometricService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @PostMapping("/register/start")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Iniciar registro de biometria", description = "Gera o desafio WebAuthn para registrar biometria no dispositivo informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desafio gerado."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public BiometricRegistrationStartResponse startRegistration(
            @Valid @RequestBody StartBiometricRegistrationRequest request,
            Authentication authentication
    ) {
        var user = requireCurrentUser(authentication);
        return biometricService.startRegistration(user, request.deviceId());
    }

    @PostMapping("/register/complete")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Concluir registro de biometria", description = "Verifica a credencial criada pelo navegador e salva a biometria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Biometria registrada."),
            @ApiResponse(responseCode = "400", description = "Resposta inválida ou verificação falhou.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public BiometricResponse completeRegistration(
            @Valid @RequestBody CompleteBiometricRegistrationRequest request,
            Authentication authentication
    ) {
        var user = requireCurrentUser(authentication);
        return biometricService.completeRegistration(user, request);
    }

    @PostMapping("/authenticate/start")
    @Operation(summary = "Iniciar autenticação por biometria", description = "Gera o desafio WebAuthn para login sem senha neste dispositivo. Não exige estar autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Desafio gerado."),
            @ApiResponse(responseCode = "404", description = "Nenhuma biometria registrada neste dispositivo.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public BiometricAuthenticationStartResponse startAuthentication(
            @Valid @RequestBody StartBiometricAuthenticationRequest request
    ) {
        return biometricService.startAuthentication(request.deviceId());
    }

    @PostMapping("/authenticate/complete")
    @Operation(summary = "Concluir autenticação por biometria", description = "Verifica a assinatura do navegador e, se válida, emite uma sessão normal (mesmo formato do login por senha).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado."),
            @ApiResponse(responseCode = "400", description = "Verificação falhou.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public LoginResponse completeAuthentication(
            @Valid @RequestBody CompleteBiometricAuthenticationRequest request
    ) {
        return biometricService.completeAuthentication(request);
    }

    @GetMapping("/status")
    @Operation(summary = "Verificar biometria do dispositivo", description = "Indica se este dispositivo já tem biometria registrada, para decidir se mostra o botão de login rápido.")
    @ApiResponse(responseCode = "200", description = "Status retornado.")
    public BiometricStatusResponse status(@RequestParam UUID deviceId) {
        return new BiometricStatusResponse(biometricService.hasBiometric(deviceId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar biometrias", description = "Lista as biometrias ativas da conta autenticada num dispositivo específico.")
    @ApiResponse(responseCode = "200", description = "Biometrias retornadas.")
    public List<BiometricResponse> list(@RequestParam UUID deviceId, Authentication authentication) {
        var user = requireCurrentUser(authentication);
        return biometricService.listBiometrics(user, deviceId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remover biometria", description = "Revoga uma biometria registrada pela conta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Biometria removida."),
            @ApiResponse(responseCode = "404", description = "Biometria não encontrada.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> remove(@PathVariable UUID id, Authentication authentication) {
        var user = requireCurrentUser(authentication);
        biometricService.removeBiometric(id, user);
        return ResponseEntity.noContent().build();
    }

    private org.application.model.User requireCurrentUser(Authentication authentication) {
        UUID userId = currentUser.id(authentication);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
    }
}
