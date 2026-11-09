package de.progle.service;

public class AiException extends RuntimeException {

    private final Integer statusCode;

    public AiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public AiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
