package org.application.config;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.application.service.validation.WebAuthnCredentialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class WebAuthnConfig {

    @Bean
    RelyingParty relyingParty(
            WebAuthnCredentialRepository credentialRepository,
            @Value("${app.webauthn.relying-party-id}") String relyingPartyId,
            @Value("${app.webauthn.relying-party-name}") String relyingPartyName,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        Set<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(relyingPartyId)
                .name(relyingPartyName)
                .build();

        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository((CredentialRepository) credentialRepository)
                .origins(origins)
                .build();
    }
}
