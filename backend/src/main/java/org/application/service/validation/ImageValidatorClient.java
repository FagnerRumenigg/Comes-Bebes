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
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
            ResponseEntity<byte[]> response;
            try {
                response = new RestTemplate().exchange(validatorUrl + "/validate", HttpMethod.POST, request, byte[].class);
            } catch (HttpStatusCodeException exception) {
                String responseBody = exception.getResponseBodyAsString(StandardCharsets.UTF_8);
                // A maioria dessas respostas é uma rejeição de validação esperada (ex.: imagem
                // sem comida), não uma falha do sistema — o corpo fica em DEBUG, não WARN.
                log.debug("event=image_validator_response_body status={} body={}",
                        exception.getStatusCode().value(), responseBody);
                throw responseError(exception.getStatusCode().value(), responseBody);
            }
            if (response.getStatusCode().value() < 200 || response.getStatusCode().value() >= 300 || response.getBody() == null) {
                log.warn("event=image_validator_rejected status={} sizeBytes={} sha256={}",
                        response.getStatusCode().value(), content.length, sha256(content));
                throw unavailable();
            }
            return toValidationResult(response);
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw unavailable();
        }
    }

    private ValidationResult toValidationResult(ResponseEntity<byte[]> response) {
        HttpHeaders responseHeaders = response.getHeaders();
        Integer width = parseIntHeader(responseHeaders, "X-Image-Width");
        Integer height = parseIntHeader(responseHeaders, "X-Image-Height");
        if (width == null || height == null || width <= 0 || height <= 0) {
            throw unavailable();
        }
        MediaType contentType = responseHeaders.getContentType();
        return new ValidationResult(
                response.getBody(),
                contentType == null ? "image/webp" : contentType.toString(),
                width,
                height,
                parseDoubleHeader(responseHeaders, "X-Food-Score"),
                parseDoubleHeader(responseHeaders, "X-Food-Threshold"),
                parseStringHeader(responseHeaders, "X-Photo-Taken-At"));
    }

    private Integer parseIntHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double parseDoubleHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        try {
            return value == null ? null : Double.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String parseStringHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        return value == null || value.isBlank() ? null : value;
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

    private InvalidOperationException responseError(int status, String responseBody) {
        String code = extractErrorCode(responseBody);
        if (status == 413 || "FILE_TOO_LARGE".equals(code)) {
            return new InvalidOperationException("IMAGE_TOO_LARGE", "A imagem excede o limite permitido.");
        }
        if ("IMAGE_NOT_FOOD".equals(code)) {
            return new InvalidOperationException("IMAGE_NOT_FOOD", "A imagem precisa apresentar uma comida.");
        }
        if (status == 422) {
            return new InvalidOperationException("INVALID_IMAGE", "O arquivo não é uma imagem válida.");
        }
        return unavailable();
    }

    private String extractErrorCode(String responseBody) {
        try {
            ErrorEnvelope envelope = objectMapper.readValue(responseBody, ErrorEnvelope.class);
            return envelope == null || envelope.detail() == null ? null : envelope.detail().code();
        } catch (Exception exception) {
            return null;
        }
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

    /**
     * photoTakenAt: ISO-8601 sem fuso (ex.: "2026-08-15T14:32:07"), lido do EXIF pelo validador
     * antes de removê-lo. Nulo quando a imagem não carrega essa informação (prints, PNGs, fotos
     * reenviadas por apps que removem metadados).
     */
    public record ValidationResult(byte[] imageBytes, String contentType, int width, int height,
                                    Double foodScore, Double threshold, String photoTakenAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorDetail(String code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorEnvelope(ErrorDetail detail) {
    }
}
