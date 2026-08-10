package org.application.service.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.application.service.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class ImageValidatorClient {
    private static final Logger log = LoggerFactory.getLogger(ImageValidatorClient.class);

    private final ObjectMapper objectMapper;

    @Value("${app.image-validator.url:http://localhost:8001}")
    private String validatorUrl;

    public ValidationResult validate(byte[] content, String filename, String contentType) {
        String safeFilename = filename == null || filename.isBlank() ? "upload.img" : filename.replaceAll("[\\r\\n\\\"]", "_");
        String requestedContentType = normalizeContentType(contentType, safeFilename, content);
        try {
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("file", new ByteArrayResource(content) {
                        @Override
                        public String getFilename() {
                            return safeFilename;
                        }
                    })
                    .contentType(MediaType.parseMediaType(requestedContentType));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, HttpEntity<?>>> request = new HttpEntity<>(body.build(), headers);
            ResponseEntity<String> response;
            try {
                response = new RestTemplate().postForEntity(validatorUrl + "/validate", request, String.class);
            } catch (HttpStatusCodeException exception) {
                log.warn("event=image_validator_response_body body={}", exception.getResponseBodyAsString());
                throw responseError(exception.getStatusCode().value());
            }
            if (response.getStatusCode().value() < 200 || response.getStatusCode().value() >= 300) {
                log.warn("event=image_validator_rejected status={} sizeBytes={} sha256={}",
                        response.getStatusCode().value(), content.length, sha256(content));
                throw responseError(response.getStatusCode().value());
            }
            ValidationResult result = objectMapper.readValue(response.getBody(), ValidationResult.class);
            if (result == null || result.classification() == null) {
                throw unavailable();
            }
            if (!"FOOD".equals(result.classification().decision())) {
                throw new InvalidOperationException("IMAGE_NOT_FOOD", "A imagem precisa apresentar uma comida.");
            }
            return result;
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw unavailable();
        }
    }

    private String normalizeContentType(String contentType, String filename, byte[] content) {
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("image/")) {
                return normalized;
            }
        }

        String extension = filename == null ? "" : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "heic", "heif" -> "image/heic";
            default -> inferFromSignature(content);
        };
    }

    private String inferFromSignature(byte[] content) {
        if (content == null || content.length < 8) {
            return "application/octet-stream";
        }
        if (content[0] == (byte) 0xFF && content[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        if (content[0] == (byte) 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return "image/png";
        }
        if (content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F') {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private InvalidOperationException responseError(int status) {
        return switch (status) {
            case 413 -> new InvalidOperationException("IMAGE_TOO_LARGE", "A imagem excede o limite permitido.");
            case 422 -> new InvalidOperationException("INVALID_IMAGE", "O arquivo não é uma imagem válida.");
            default -> unavailable();
        };
    }

    private InvalidOperationException unavailable() {
        return new InvalidOperationException("IMAGE_VALIDATOR_UNAVAILABLE", "O validador de imagens está indisponível.");
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidationResult(String status, Classification classification) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Classification(String decision, Double food_score, Double threshold) {
    }
}
