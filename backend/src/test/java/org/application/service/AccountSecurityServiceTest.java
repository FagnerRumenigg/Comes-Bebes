package org.application.service;

import org.application.model.User;
import org.application.model.UserRole;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.application.util.StringNormalizer;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StringNormalizer normalizer;
    @Mock private Clock clock;
    @InjectMocks private AccountSecurityService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hmacSecret", "test-secret");
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-08-08T15:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldBlockUserByAdministratorAndPreserveUsernameHmac() {
        UUID administratorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User admin = User.builder().id(administratorId).role(UserRole.ADMIN).build();
        User user = User.builder().id(userId).email("User@Example.com").username("User_Name").build();
        when(userRepository.findById(administratorId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(normalizer.normalize("User_Name")).thenReturn("user_name");

        service.block(userId, administratorId, "violação das regras");

        assertThat(user.getStatus().name()).isEqualTo("BLOCKED");
        assertThat(user.getBlockedUsernameHmac()).isNotBlank();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getBlockedBy()).isEqualTo(administratorId);
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectNonAdministratorBlock() {
        UUID administratorId = UUID.randomUUID();
        when(userRepository.findById(administratorId)).thenReturn(Optional.of(User.builder()
                .id(administratorId).role(UserRole.USER).build()));

        assertThatThrownBy(() -> service.block(UUID.randomUUID(), administratorId, "reason"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldAnonymizeAccount() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").username("user").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.anonymize(userId);

        assertThat(user.getStatus().name()).isEqualTo("DELETED");
        assertThat(user.getEmail()).isNull();
        verify(userRepository).save(user);
    }
}
