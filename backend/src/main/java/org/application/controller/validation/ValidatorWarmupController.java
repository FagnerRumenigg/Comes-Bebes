package org.application.controller.validation;

import lombok.RequiredArgsConstructor;
import org.application.service.validation.ImageValidatorClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/validator")
public class ValidatorWarmupController {
    private final ImageValidatorClient imageValidatorClient;

    @PostMapping("/warmup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> warmup() {
        imageValidatorClient.warmUp();
        return ResponseEntity.noContent().build();
    }
}
