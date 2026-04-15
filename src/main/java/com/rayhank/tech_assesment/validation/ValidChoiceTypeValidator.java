package com.rayhank.tech_assesment.validation;

import com.rayhank.tech_assesment.entity.ChoiceType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidChoiceTypeValidator implements ConstraintValidator<ValidChoiceType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Delegate null/blank check to @NotBlank
        if (value == null || value.isBlank()) return true;
        return ChoiceType.fromDisplayName(value) != null;
    }
}
