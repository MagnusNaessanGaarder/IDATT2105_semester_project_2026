package com.example.InternalControl.validation.annotation;

import com.example.InternalControl.validation.validator.OrganizationNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a value is a valid Norwegian organization number (org.nr.).
 * Validates format (9 digits) and modulus 11 check digit.
 */
@Constraint(validatedBy = OrganizationNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrganizationNumber {

    String message() default "Invalid organization number format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
