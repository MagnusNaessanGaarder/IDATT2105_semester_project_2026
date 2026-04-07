package com.example.InternalControl.validation.validator;

import com.example.InternalControl.validation.annotation.NotFutureDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Validator that ensures a date is not in the future.
 * Supports LocalDate, LocalDateTime, and java.util.Date.
 */
public class NotFutureDateValidator implements ConstraintValidator<NotFutureDate, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Null values are handled by @NotNull
        }

        if (value instanceof LocalDate) {
            return !((LocalDate) value).isAfter(LocalDate.now());
        } else if (value instanceof LocalDateTime) {
            return !((LocalDateTime) value).isAfter(LocalDateTime.now());
        } else if (value instanceof Date) {
            return !((Date) value).after(new Date());
        }

        // Unsupported type
        return false;
    }
}
