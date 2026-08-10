package org.application.controller;

import org.application.controller.response.ApiErrorResponse;
import org.application.service.exception.DuplicateResourceException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DuplicateResourceException exception) {
        return response(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
    }

    @ExceptionHandler({InvalidOperationException.class, MethodArgumentNotValidException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        Map<String, String> fieldErrors = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new))
                : null;
        String message = exception instanceof MethodArgumentNotValidException validationException
                ? fieldErrors.entrySet().stream()
                .findFirst()
                .map(error -> error.getKey() + ": " + error.getValue())
                .orElse("Requisição inválida.")
                : exception instanceof MissingServletRequestPartException missingPart
                ? "Parte multipart obrigatória ausente: " + missingPart.getRequestPartName() + "."
                : exception.getMessage();
        String code = exception instanceof InvalidOperationException invalid ? invalid.code() : "VALIDATION_ERROR";
        HttpStatus status = switch (code) {
            case "IMAGE_TOO_LARGE" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "IMAGE_VALIDATOR_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return response(status, code, message, fieldErrors);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException exception) {
        return response(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "A imagem excede o limite de 15 MB.");
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message) {
        return response(status, code, message, null);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .fieldErrors(fieldErrors)
                .build());
    }
}
