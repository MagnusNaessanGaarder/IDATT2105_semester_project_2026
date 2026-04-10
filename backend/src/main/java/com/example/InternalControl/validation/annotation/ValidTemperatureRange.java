package com.example.InternalControl.validation.annotation;

import com.example.InternalControl.validation.validator.TemperatureRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a temperature value is within acceptable range for food safety.
 * Default range: -50°C to +100°C
 * Can be customized with min and max parameters.
 */
@Constraint(validatedBy = TemperatureRangeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTemperatureRange {

    String message() default "Temperature must be between -50.0°C and 100.0°C";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum allowed temperature in Celsius.
     * Default: -50.0 (for freezer readings)
     */
    double min() default -50.0;

    /**
     * Maximum allowed temperature in Celsius.
     * Default: 100.0 (for hot food)
     */
    double max() default 100.0;
}
