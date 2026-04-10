package com.example.InternalControl.validation;

import com.example.InternalControl.validation.annotation.ValidTemperatureRange;
import com.example.InternalControl.validation.validator.TemperatureRangeValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TemperatureRangeValidator.
 */
class TemperatureRangeValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    static class TestTemperature {
        private Double temperature;

        private Double fridgeTemp;

        TestTemperature(Double temperature) {
            this.temperature = temperature;
        }

        TestTemperature(Double temperature, Double fridgeTemp) {
            this.temperature = temperature;
            this.fridgeTemp = fridgeTemp;
        }

        @ValidTemperatureRange
        public Double getTemperature() {
            return temperature;
        }

        @ValidTemperatureRange(min = 0.0, max = 10.0)
        public Double getFridgeTemp() {
            return fridgeTemp;
        }
    }

    @Test
    void validTemperature_ShouldPass() {
        TestTemperature test = new TestTemperature(25.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validFreezerTemperature_ShouldPass() {
        TestTemperature test = new TestTemperature(-20.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validHotFoodTemperature_ShouldPass() {
        TestTemperature test = new TestTemperature(85.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void tooLowTemperature_ShouldFail() {
        TestTemperature test = new TestTemperature(-60.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("between -50.0°C and 100.0°C")));
    }

    @Test
    void tooHighTemperature_ShouldFail() {
        TestTemperature test = new TestTemperature(150.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("between -50.0°C and 100.0°C")));
    }

    @Test
    void nullTemperature_ShouldPass() {
        TestTemperature test = new TestTemperature(null);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validTemperatureWithBigDecimal_ShouldPass() {
        // Test that BigDecimal is also supported
        TemperatureRangeValidator validator = new TemperatureRangeValidator();
        validator.initialize(createAnnotation(-50.0, 100.0));

        boolean valid = validator.isValid(new BigDecimal("25.5"), null);
        assertTrue(valid);
    }

    @Test
    void boundaryMinTemperature_ShouldPass() {
        TestTemperature test = new TestTemperature(-50.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void boundaryMaxTemperature_ShouldPass() {
        TestTemperature test = new TestTemperature(100.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void customRange_ShouldPass() {
        TestTemperature test = new TestTemperature(0.0, 5.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void customRangeOutOfBounds_ShouldFail() {
        TestTemperature test = new TestTemperature(0.0, 15.0);
        Set<ConstraintViolation<TestTemperature>> violations = validator.validate(test);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fridgeTemp")));
    }

    @Test
    void invalidInitializer_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TemperatureRangeValidator validator = new TemperatureRangeValidator();
            validator.initialize(createAnnotation(100.0, -50.0)); // min > max
        });
    }

    private ValidTemperatureRange createAnnotation(double min, double max) {
        return new ValidTemperatureRange() {
            @Override
            public String message() { return "test"; }
            @Override
            public Class<?>[] groups() { return new Class[0]; }
            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
            @Override
            public double min() { return min; }
            @Override
            public double max() { return max; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ValidTemperatureRange.class;
            }
        };
    }
}
