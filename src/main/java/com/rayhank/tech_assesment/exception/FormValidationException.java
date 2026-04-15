package com.rayhank.tech_assesment.exception;

import java.util.List;
import java.util.Map;

// Thrown when service-level validation fails — produces 422 "Invalid field" with field errors
public class FormValidationException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public FormValidationException(Map<String, List<String>> errors) {
        super("Invalid field");
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
