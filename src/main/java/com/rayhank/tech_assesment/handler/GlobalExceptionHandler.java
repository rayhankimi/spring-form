package com.rayhank.tech_assesment.handler;

import com.rayhank.tech_assesment.dto.MessageResponse;
import com.rayhank.tech_assesment.exception.*;
import com.rayhank.tech_assesment.exception.ForbiddenAccessException;
import com.rayhank.tech_assesment.exception.FormNotFoundException;
import com.rayhank.tech_assesment.exception.QuestionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid failures → 422 Unprocessable Entity
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String fieldName = toSnakeCase(fieldError.getField());
            errors
                .computeIfAbsent(fieldName, k -> new ArrayList<>())
                .add(fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Invalid field");
        body.put("errors", errors);
        return body;
    }

    // Handles wrong email/password from AuthenticationManager → 401
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public MessageResponse handleBadCredentials(BadCredentialsException ex) {
        return new MessageResponse("Email or password incorrect");
    }

    // Converts camelCase field names to snake_case for consistent JSON error keys
    // e.g. choiceType → choice_type, allowedDomains → allowed_domains
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    // Form slug not found → 404
    @ExceptionHandler(FormNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public MessageResponse handleFormNotFound(FormNotFoundException ex) {
        return new MessageResponse(ex.getMessage());
    }

    // Question id not found (or not belonging to the form) → 404
    @ExceptionHandler(QuestionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public MessageResponse handleQuestionNotFound(QuestionNotFoundException ex) {
        return new MessageResponse(ex.getMessage());
    }

    // Service-level field validation (e.g. required answers) → 422 "Invalid field"
    @ExceptionHandler(FormValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleFormValidation(FormValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Invalid field");
        body.put("errors", ex.getErrors());
        return body;
    }

    // Duplicate submission when limit_one_response is enabled → 422
    @ExceptionHandler(AlreadySubmittedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public MessageResponse handleAlreadySubmitted(AlreadySubmittedException ex) {
        return new MessageResponse(ex.getMessage());
    }

    // User email domain not allowed to access the form → 403
    @ExceptionHandler(ForbiddenAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public MessageResponse handleForbiddenAccess(ForbiddenAccessException ex) {
        return new MessageResponse(ex.getMessage());
    }
}
