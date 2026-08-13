package org.application.controller.user;

import org.application.controller.ApiExceptionHandler;
import org.application.controller.user.request.CreateUserRequest;
import org.application.model.User;
import org.application.service.UserService;
import org.application.service.PublicationService;
import org.application.service.PublicationResponseFactory;
import org.application.config.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.UUID;
import java.time.ZoneId;
import org.springframework.data.domain.PageImpl;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PublicationService publicationService;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private PublicationResponseFactory responseFactory;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(
                        userService,
                        publicationService,
                        ZoneId.of("America/Sao_Paulo"),
                        org.mockito.Mockito.mock(org.application.service.AccountSecurityService.class)
                        , currentUser,
                        responseFactory
                ))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        org.mockito.Mockito.lenient().when(currentUser.id(any(), any(UUID.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void shouldFindUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findActive(id)).thenReturn(user(id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.update(any(UUID.class), any(org.application.controller.user.request.UpdateUserRequest.class)))
                .thenReturn(user(id));

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Novo nome\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void shouldListProfilePublications() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicationService.profile(any(UUID.class), any(org.springframework.data.domain.Pageable.class), nullable(UUID.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/users/{id}/publications", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldListNotifications() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.notifications(any(UUID.class), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/users/{id}/notifications", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldAnonymizeAccount() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/users/{id}/account", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldBlockUser() throws Exception {
        UUID id = UUID.randomUUID();
        UUID administratorId = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/block", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"administratorId":"%s","reason":"violação das regras"}
                                """.formatted(administratorId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldCompleteOnboarding() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/onboarding", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldMarkPatchNotesSeen() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/patch-notes/seen", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldChangePassword() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/users/{id}/password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isNoContent());
    }

    private User user(UUID id) {
        return User.builder()
                .id(id)
                .email("fagner@example.com")
                .passwordHash("hash")
                .username("fagner")
                .displayName("Fagner")
                .build();
    }
}
