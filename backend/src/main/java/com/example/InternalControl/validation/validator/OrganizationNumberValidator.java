package com.example.InternalControl.validation.validator;

import com.example.InternalControl.validation.annotation.ValidOrganizationNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Norwegian organization numbers (org.nr.).
 * Validates:
 * 1. Format: exactly 9 digits
 * 2. Modulus 11 check digit calculation
 *
 * The Norwegian organization number uses a weighted sum algorithm:
 * - Weights: 3, 2, 7, 6, 5, 4, 3, 2
 * - Sum the products of each digit with its weight
 * - Remainder when divided by 11
 * - Check digit = 11 - remainder (if remainder is 0, check digit is 0)
 * - If result is 10, the number is invalid
 */
public class OrganizationNumberValidator implements ConstraintValidator<ValidOrganizationNumber, Object> {

    private static final int[] WEIGHTS = {3, 2, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Null values are handled by @NotNull
        }

        String orgNumber;
        if (value instanceof Integer) {
            orgNumber = String.valueOf((Integer) value);
        } else if (value instanceof Long) {
            orgNumber = String.valueOf((Long) value);
        } else if (value instanceof String) {
            orgNumber = (String) value;
        } else {
            return false;
        }

        // Remove any whitespace
        orgNumber = orgNumber.trim();

        // Must be exactly 9 digits
        if (!orgNumber.matches("^\\d{9}$")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Organization number must be exactly 9 digits"
            ).addConstraintViolation();
            return false;
        }

        // Calculate modulus 11 check
        if (!isValidModulus11(orgNumber)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid organization number (failed checksum validation)"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Performs the modulus 11 check for Norwegian organization numbers.
     */
    private boolean isValidModulus11(String orgNumber) {
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            int digit = Character.getNumericValue(orgNumber.charAt(i));
            sum += digit * WEIGHTS[i];
        }

        int remainder = sum % 11;
        int checkDigit;

        if (remainder == 0) {
            checkDigit = 0;
        } else if (remainder == 1) {
            // If remainder is 1, the check digit would be 10, which is invalid
            return false;
        } else {
            checkDigit = 11 - remainder;
        }

        int actualCheckDigit = Character.getNumericValue(orgNumber.charAt(8));
        return checkDigit == actualCheckDigit;
    }
}
