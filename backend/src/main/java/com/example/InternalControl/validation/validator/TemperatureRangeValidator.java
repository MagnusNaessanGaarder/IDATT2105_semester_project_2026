package com.example.InternalControl.validation.validator;

import com.example.InternalControl.validation.annotation.ValidTemperatureRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Validator for temperature range validation.
 * Accepts both Double and BigDecimal values.
 */
public class TemperatureRangeValidator implements ConstraintValidator<ValidTemperatureRange, Object> {

    private double min;
    private double max;

    @Override
    public void initialize(ValidTemperatureRange constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();

        // Validate that min < max
        if (min >= max) {
            throw new IllegalArgumentException("Minimum temperature must be less than maximum temperature");
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Null values are handled by @NotNull
        }

        double temperature;
        if (value instanceof Double) {
            temperature = (Double) value;
        } else if (value instanceof BigDecimal) {
            temperature = ((BigDecimal) value).doubleValue();
        } else if (value instanceof Number) {
            temperature = ((Number) value).doubleValue();
        } else {
            return false;
        }

        // Check for NaN or Infinity
        if (Double.isNaN(temperature) || Double.isInfinite(temperature)) {
            return false;
        }

        boolean valid = temperature >= min && temperature <= max;

        if (!valid && context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format(Locale.ROOT, "Temperature must be between %.1f°C and %.1f°C", min, max)
            ).addConstraintViolation();
        }

        return valid;
    }
}
