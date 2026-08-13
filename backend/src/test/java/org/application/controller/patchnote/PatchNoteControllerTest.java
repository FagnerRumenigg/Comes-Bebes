package org.application.controller.patchnote;

import org.application.config.CurrentUser;
import org.application.controller.ApiExceptionHandler;
import org.application.model.PatchNote;
import org.application.model.User;
import org.application.service.PatchNoteService;
import org.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatchNoteControllerTest {

    @Mock
    private PatchNoteService patchNoteService;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PatchNoteController(patchNoteService, userService, currentUser))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldListUnseenPatchNotesForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        PatchNote note = PatchNote.builder()
                .id(UUID.randomUUID())
                .title("Notas de versão")
                .body("Agora dá para acompanhar as novidades por aqui.")
                .publishedAt(OffsetDateTime.now())
                .build();

        when(currentUser.id(any())).thenReturn(userId);
        when(userService.findActive(userId)).thenReturn(user);
        when(patchNoteService.findUnseen(user)).thenReturn(List.of(note));

        mockMvc.perform(get("/patch-notes/unseen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Notas de versão"))
                .andExpect(jsonPath("$[0].body").value("Agora dá para acompanhar as novidades por aqui."));
    }

    @Test
    void shouldReturnEmptyListWhenThereIsNothingUnseen() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        when(currentUser.id(any())).thenReturn(userId);
        when(userService.findActive(userId)).thenReturn(user);
        when(patchNoteService.findUnseen(user)).thenReturn(List.of());

        mockMvc.perform(get("/patch-notes/unseen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
