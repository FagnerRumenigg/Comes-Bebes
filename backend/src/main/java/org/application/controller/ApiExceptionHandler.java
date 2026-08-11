package org.application.controller;

import org.application.controller.response.ApiErrorResponse;
import org.application.service.exception.DuplicateResourceException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "A imagem excede o limite de 20 MB.");
    }

    // Os handlers abaixo preservam o status HTTP que o Spring já resolveria por padrão para
    // esses casos comuns. São declarados explicitamente para que o handler genérico de
    // Exception.class (que precisa existir para logar e formatar erros 500 de verdade) não os
    // capture antes e os transforme incorretamente em 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRouteNotFound(NoResourceFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", exception.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "A requisição não pôde ser interpretada.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("event=unhandled_exception", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocorreu um erro inesperado.");
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
