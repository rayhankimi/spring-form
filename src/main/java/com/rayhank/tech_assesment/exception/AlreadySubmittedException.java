package com.rayhank.tech_assesment.exception;

public class AlreadySubmittedException extends RuntimeException {
    public AlreadySubmittedException() {
        super("You can not submit form twice");
    }
}
