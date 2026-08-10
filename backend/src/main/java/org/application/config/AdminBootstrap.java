package org.application.config;

import lombok.RequiredArgsConstructor;
import org.application.dto.UserData;
import org.application.model.User;
import org.application.model.UserRole;
import org.application.repository.UserRepository;
import org.application.util.StringNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrap {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringNormalizer normalizer;

    @Bean
    @ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
    ApplicationRunner createLocalAdmin(
            @Value("${app.bootstrap-admin.password}") String password,
            @Value("${app.bootstrap-admin.username}") String username,
            @Value("${app.bootstrap-admin.display-name}") String displayName
    ) {
        return arguments -> {
            String normalizedUsername = normalizer.normalize(username);
            if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) {
                return;
            }
            User admin = User.of(UserData.builder()
                    .email(null)
                    .passwordHash(passwordEncoder.encode(password))
                    .username(normalizedUsername)
                    .displayName(displayName.trim())
                    .build());
            admin = User.builder()
                    .id(admin.getId())
                    .email(null)
                    .passwordHash(admin.getPasswordHash())
                    .username(admin.getUsername())
                    .displayName(admin.getDisplayName())
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
        };
    }
}
