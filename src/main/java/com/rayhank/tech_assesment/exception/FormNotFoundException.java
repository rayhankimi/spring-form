package com.rayhank.tech_assesment.exception;

public class FormNotFoundException extends RuntimeException {
    public FormNotFoundException() {
        super("Form not found");
    }
}
