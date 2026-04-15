package com.rayhank.tech_assesment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueSlugValidator.class)
public @interface UniqueSlug {
    String message() default "The slug has already been taken.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
