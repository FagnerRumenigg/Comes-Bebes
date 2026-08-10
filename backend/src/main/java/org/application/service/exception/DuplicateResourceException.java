package org.application.service.exception;

public class DuplicateResourceException extends RuntimeException {
    private final String code;

    public DuplicateResourceException(String message) {
        this("RESOURCE_CONFLICT", message);
    }

    public DuplicateResourceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
