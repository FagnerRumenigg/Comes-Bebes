package org.application.config;

import org.application.service.exception.InvalidOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {

    public UUID id(Authentication authentication, UUID legacyId) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            if (legacyId != null) {
                return legacyId;
            }
            throw new InvalidOperationException("Usuário autenticado não encontrado.");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new InvalidOperationException("Identidade autenticada inválida.");
        }
    }

    public UUID id(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return id(authentication, null);
    }
}
