package org.application.dto;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record UserData(
        String email,
        String passwordHash,
        String username,
        String displayName,
        LocalDate dateOfBirth
) {
}
