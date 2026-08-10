package org.application.controller.moderation;

import org.application.service.ModerationService;
import org.application.config.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ModerationControllerTest {

    @Mock
    private ModerationService moderationService;

    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ModerationController(moderationService, currentUser)).build();
    }

    @Test
    void shouldListPendingCases() throws Exception {
        when(moderationService.pendingCases()).thenReturn(List.of());

        mockMvc.perform(get("/moderation/cases"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDecideModerationCase() throws Exception {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(currentUser.id(any(), any(java.util.UUID.class))).thenAnswer(invocation -> invocation.getArgument(1));
        when(moderationService.decide(any(java.util.UUID.class), any(org.application.controller.moderation.request.DecideModerationCaseRequest.class), any(java.util.UUID.class)))
                .thenReturn(org.application.model.ModerationCase.builder().id(id).publicationId(java.util.UUID.randomUUID()).status("KEPT").build());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/moderation/cases/{id}", id)
                        .contentType("application/json")
                        .content("{\"reviewerId\":\"" + java.util.UUID.randomUUID() + "\",\"decision\":\"KEPT\"}"))
                .andExpect(status().isOk());
    }
}
