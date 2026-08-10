package org.application.dto;

import lombok.Builder;

@Builder
public record UserData(
        String email,
        String passwordHash,
        String username,
        String displayName
) {
}
