package com.rayhank.tech_assesment.validation;

import com.rayhank.tech_assesment.dto.form.AddQuestionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class ChoicesRequiredForTypeValidator
        implements ConstraintValidator<ChoicesRequiredForType, AddQuestionRequest> {

    private static final Set<String> TYPES_REQUIRING_CHOICES = Set.of(
            "multiple choice", "dropdown", "checkboxes"
    );

    @Override
    public boolean isValid(AddQuestionRequest request, ConstraintValidatorContext context) {
        String choiceType = request.getChoiceType();

        // If choice_type is null/blank/invalid, let field-level validators handle it
        if (choiceType == null || choiceType.isBlank()) return true;
        if (!TYPES_REQUIRING_CHOICES.contains(choiceType.toLowerCase().trim())) return true;

        boolean choicesEmpty = request.getChoices() == null || request.getChoices().isEmpty();
        if (choicesEmpty) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The choices field is required.")
                    .addPropertyNode("choices")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
