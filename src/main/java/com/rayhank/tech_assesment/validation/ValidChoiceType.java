package com.rayhank.tech_assesment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidChoiceTypeValidator.class)
public @interface ValidChoiceType {
    String message() default "The choice type is invalid.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
