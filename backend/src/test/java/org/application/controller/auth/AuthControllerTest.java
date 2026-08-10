package org.application.controller.auth;

import org.application.controller.ApiExceptionHandler;
import org.application.controller.auth.request.LoginRequest;
import org.application.controller.auth.request.RefreshTokenRequest;
import org.application.controller.auth.response.LoginResponse;
import org.application.controller.user.request.CreateUserRequest;
import org.application.controller.user.response.UserResponse;
import org.application.service.AuthService;
import org.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthService authService;
    @Mock private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, userService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        when(authService.login(any(LoginRequest.class), any(String.class))).thenReturn(
                new LoginResponse("token", "refresh", "Bearer", 3600, UUID.randomUUID(), "fagner", org.application.model.UserRole.USER, OffsetDateTime.now()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fagner\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    void shouldRejectInvalidLoginPayload() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRegisterUser() throws Exception {
        UUID id = UUID.randomUUID();
        var user = org.application.model.User.builder().id(id).username("fagner").displayName("Fagner")
                .role(org.application.model.UserRole.USER).status(org.application.model.UserStatus.ACTIVE)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
        when(userService.create(any(CreateUserRequest.class))).thenReturn(user);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password\",\"username\":\"fagner\",\"displayName\":\"Fagner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void shouldRefreshSession() throws Exception {
        when(authService.refresh("refresh")).thenReturn(
                new LoginResponse("token-2", "refresh-2", "Bearer", 3600, UUID.randomUUID(), "fagner", org.application.model.UserRole.USER, OffsetDateTime.now()));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-2"));
    }

    @Test
    void shouldLogoutSession() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh");
    }
}
