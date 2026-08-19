package org.application.controller.image;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.application.service.storage.ImageStorage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.util.concurrent.TimeUnit;

/**
 * Serve as imagens armazenadas via ImageStorage (disco local ou Azure Blob,
 * conforme app.storage.type) — nunca um link direto/público pro storage em si.
 * Substitui o antigo ResourceHttpRequestHandler estático, que só funcionava
 * apontando pro filesystem local e não tem equivalente pra Blob Storage.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class ImageController {

    private static final String PATTERN = "/images/**";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ImageStorage imageStorage;

    @GetMapping(PATTERN)
    public ResponseEntity<byte[]> serve(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String objectName = PATH_MATCHER.extractPathWithinPattern(PATTERN, path);
        byte[] content = imageStorage.read(objectName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/webp"))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(content);
    }
}
