package org.application.controller.user;

import org.application.config.CurrentUser;
import org.application.controller.ApiExceptionHandler;
import org.application.model.User;
import org.application.model.UserRole;
import org.application.model.UserStatus;
import org.application.service.AccountSecurityService;
import org.application.service.CollectionService;
import org.application.service.FollowService;
import org.application.service.PublicationResponseFactory;
import org.application.service.PublicationService;
import org.application.service.UserService;
import org.application.service.exception.InvalidOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock private UserService userService;
    @Mock private PublicationService publicationService;
    @Mock private AccountSecurityService accountSecurityService;
    @Mock private FollowService followService;
    @Mock private CollectionService collectionService;
    @Mock private CurrentUser currentUser;
    @Mock private PublicationResponseFactory responseFactory;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService, publicationService, ZoneId.of("UTC"),
                accountSecurityService, followService, collectionService, currentUser, responseFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private User user(UUID id) {
        return User.builder().id(id).username("fagner").displayName("Fagner").role(UserRole.USER).status(UserStatus.ACTIVE).build();
    }

    @Test
    void shouldFollowUser() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(followerId);

        mockMvc.perform(put("/users/{id}/follow", followedId))
                .andExpect(status().isNoContent());

        verify(followService).follow(followerId, followedId);
    }

    @Test
    void shouldRejectFollowingSelfWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(userId);
        doThrow(new InvalidOperationException("CANNOT_FOLLOW_SELF", "Você não pode seguir a si mesmo."))
                .when(followService).follow(userId, userId);

        mockMvc.perform(put("/users/{id}/follow", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_FOLLOW_SELF"));
    }

    @Test
    void shouldUnfollowUser() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(currentUser.id(any())).thenReturn(followerId);

        mockMvc.perform(delete("/users/{id}/follow", followedId))
                .andExpect(status().isNoContent());

        verify(followService).unfollow(followerId, followedId);
    }

    @Test
    void shouldReturnFollowersPage() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        Page<User> page = new PageImpl<>(List.of(user(followerId)));
        when(followService.listFollowers(eq(profileId), any())).thenReturn(page);

        mockMvc.perform(get("/users/{id}/followers", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(followerId.toString()));
    }

    @Test
    void shouldReturnFollowingPage() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        Page<User> page = new PageImpl<>(List.of(user(followedId)));
        when(followService.listFollowing(eq(profileId), any())).thenReturn(page);

        mockMvc.perform(get("/users/{id}/following", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(followedId.toString()));
    }

    @Test
    void shouldNotExposeFollowCountsWhenFindingUser() throws Exception {
        // Nenhum contador público em lugar nenhum do produto (produto5.md v5 §3.1) —
        // seguidores/seguindo nunca aparecem na resposta, nem para o próprio dono.
        UUID profileId = UUID.randomUUID();
        when(userService.findActive(profileId)).thenReturn(user(profileId));

        mockMvc.perform(get("/users/{id}", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followersCount").doesNotExist())
                .andExpect(jsonPath("$.followingCount").doesNotExist())
                .andExpect(jsonPath("$.followedByCurrentUser").doesNotExist());
    }

    @Test
    void shouldReportFollowedByCurrentUserWhenViewerIsAuthenticated() throws Exception {
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        var authentication = new UsernamePasswordAuthenticationToken(viewerId.toString(), null, List.of());
        when(userService.findActive(profileId)).thenReturn(user(profileId));
        when(currentUser.id(authentication)).thenReturn(viewerId);
        when(followService.isFollowing(viewerId, profileId)).thenReturn(true);

        mockMvc.perform(get("/users/{id}", profileId).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedByCurrentUser").value(true));
    }

    @Test
    void shouldReturnNotificationPreferences() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUser.id(any(), eq(userId))).thenReturn(userId);
        when(userService.findActive(userId)).thenReturn(
                User.builder().id(userId).username("fagner").notifyOnFollowedPublish(false).build());

        mockMvc.perform(get("/users/{id}/notification-preferences", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyOnFollowedPublish").value(false));
    }

    @Test
    void shouldUpdateNotificationPreferences() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUser.id(any(), eq(userId))).thenReturn(userId);

        mockMvc.perform(patch("/users/{id}/notification-preferences", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyOnFollowedPublish\":false}"))
                .andExpect(status().isNoContent());

        verify(userService).updateNotifyOnFollowedPublish(userId, false);
    }
}
