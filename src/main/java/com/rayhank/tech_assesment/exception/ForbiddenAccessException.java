package com.rayhank.tech_assesment.exception;

public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException() {
        super("Forbidden access");
    }
}
