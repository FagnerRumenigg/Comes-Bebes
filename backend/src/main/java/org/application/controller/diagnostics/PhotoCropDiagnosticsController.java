package org.application.controller.diagnostics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/diagnostics/photo-crop")
public class PhotoCropDiagnosticsController {
    private static final Logger log = LoggerFactory.getLogger(PhotoCropDiagnosticsController.class);

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody PhotoCropDiagnosticsRequest request) {
        log.info("photo-crop diagnostic: {}", request);
        return ResponseEntity.noContent().build();
    }

    public record PhotoCropDiagnosticsRequest(
            @Size(max = 512) String sessionId,
            @Size(max = 1024) String userAgent,
            Integer screenWidth,
            Integer screenHeight,
            Integer viewportWidth,
            Integer viewportHeight,
            Double devicePixelRatio,
            @Size(max = 128) String fileType,
            @Size(max = 256) String fileName,
            Long fileSize,
            Integer imageWidth,
            Integer imageHeight,
            Long elapsedMs,
            Boolean cropperReady,
            Integer cropperWidth,
            Integer cropperHeight,
            @Size(max = 128) String event,
            @Size(max = 2000) String error
    ) {}
}
