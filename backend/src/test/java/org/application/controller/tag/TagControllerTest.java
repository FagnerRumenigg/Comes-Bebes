package org.application.controller.tag;

import org.application.model.Tag;
import org.application.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock private TagService tagService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TagController(tagService)).build();
    }

    @Test
    void shouldReturnMatchingTagsOrderedByOfficialFirst() throws Exception {
        Tag official = Tag.builder().id(UUID.randomUUID()).name("Açaí").slug("acai").official(true).build();
        Tag unofficial = Tag.builder().id(UUID.randomUUID()).name("Acarajé").slug("acaraje").official(false).build();
        when(tagService.search(eq("aca"), eq(10))).thenReturn(List.of(official, unofficial));

        mockMvc.perform(get("/tags/search").param("q", "aca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("acai"))
                .andExpect(jsonPath("$[0].official").value(true))
                .andExpect(jsonPath("$[1].slug").value("acaraje"))
                .andExpect(jsonPath("$[1].official").value(false));
    }

    @Test
    void shouldReturnEmptyListWhenNoTagMatches() throws Exception {
        when(tagService.search(eq("xyz"), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/tags/search").param("q", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
