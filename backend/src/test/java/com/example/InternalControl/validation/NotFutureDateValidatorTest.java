package com.example.InternalControl.validation;

import com.example.InternalControl.validation.annotation.NotFutureDate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NotFutureDateValidator.
 */
class NotFutureDateValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    static class TestDates {
        @NotFutureDate
        LocalDate localDate;

        @NotFutureDate
        LocalDateTime localDateTime;

        @NotFutureDate
        Date utilDate;

        TestDates(LocalDate localDate) {
            this.localDate = localDate;
        }

        TestDates(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        TestDates(Date utilDate) {
            this.utilDate = utilDate;
        }
    }

    @Test
    void validPastLocalDate_ShouldPass() {
        TestDates test = new TestDates(LocalDate.now().minusDays(1));
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validTodayLocalDate_ShouldPass() {
        TestDates test = new TestDates(LocalDate.now());
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void futureLocalDate_ShouldFail() {
        TestDates test = new TestDates(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("future")));
    }

    @Test
    void validPastLocalDateTime_ShouldPass() {
        TestDates test = new TestDates(LocalDateTime.now().minusHours(1));
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void futureLocalDateTime_ShouldFail() {
        TestDates test = new TestDates(LocalDateTime.now().plusHours(1));
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validPastUtilDate_ShouldPass() {
        TestDates test = new TestDates(new Date(System.currentTimeMillis() - 86400000)); // Yesterday
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void futureUtilDate_ShouldFail() {
        TestDates test = new TestDates(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertFalse(violations.isEmpty());
    }

    @Test
    void nullDate_ShouldPass() {
        TestDates test = new TestDates((LocalDate) null);
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty(), "Null values should pass (use @NotNull for required)");
    }

    @Test
    void nullLocalDateTime_ShouldPass() {
        TestDates test = new TestDates((LocalDateTime) null);
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullUtilDate_ShouldPass() {
        TestDates test = new TestDates((Date) null);
        Set<ConstraintViolation<TestDates>> violations = validator.validate(test);
        assertTrue(violations.isEmpty());
    }
}
