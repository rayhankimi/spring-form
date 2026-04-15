package com.rayhank.tech_assesment.validation;

import com.rayhank.tech_assesment.repository.FormRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Spring Boot wires FormRepository into this validator via SpringConstraintValidatorFactory
@Component
@RequiredArgsConstructor
public class UniqueSlugValidator implements ConstraintValidator<UniqueSlug, String> {

    private final FormRepository formRepository;

    @Override
    public boolean isValid(String slug, ConstraintValidatorContext context) {
        // Delegate null/blank check to @NotBlank — return true so errors don't stack
        if (slug == null || slug.isBlank()) return true;
        return !formRepository.existsBySlug(slug);
    }
}
