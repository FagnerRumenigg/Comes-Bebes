package org.application.controller.image;

import org.application.controller.ApiExceptionHandler;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    @Mock private ImageStorage imageStorage;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ImageController(imageStorage))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldServeStoredImageBytesWithWebpContentType() throws Exception {
        byte[] content = {1, 2, 3, 4};
        // objectName gravado hoje inclui o prefixo "images/" (LocalImageStorage/
        // BlobImageStorage), então a URL final tem o segmento duplicado.
        when(imageStorage.read(eq("images/foo.webp"))).thenReturn(content);

        mockMvc.perform(get("/images/images/foo.webp"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/webp"))
                .andExpect(content().bytes(content));
    }

    @Test
    void shouldReturn404WhenImageDoesNotExist() throws Exception {
        when(imageStorage.read(eq("images/missing.webp")))
                .thenThrow(new ResourceNotFoundException("IMAGE_NOT_FOUND", "Imagem não encontrada."));

        mockMvc.perform(get("/images/images/missing.webp"))
                .andExpect(status().isNotFound());
    }
}
