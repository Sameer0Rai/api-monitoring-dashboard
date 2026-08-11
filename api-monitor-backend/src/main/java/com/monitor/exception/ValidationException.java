package com.monitor.exception;

/**
 * For validation rules that depend on runtime configuration (e.g. interval bounds coming
 * from {@code MonitorProperties}) rather than a fixed constant a {@code @Min}/{@code @Max}
 * annotation could express. Carries a field name so the response can point at exactly what
 * was wrong, in the same shape as a {@code MethodArgumentNotValidException} response.
 */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
