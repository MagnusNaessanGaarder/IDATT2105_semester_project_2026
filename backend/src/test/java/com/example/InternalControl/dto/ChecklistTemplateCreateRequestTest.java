package com.example.InternalControl.dto;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.shared.enums.Frequency;
import com.example.InternalControl.shared.enums.ModuleType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DTO validation tests for ChecklistTemplateCreateRequest.
 *
 * @author TriTacLe
 * @since 1.0
 */
class ChecklistTemplateCreateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRequest_NoViolations() {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("Test Template")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullTitle_HasViolation() {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title(null)
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
    }

    @Test
    void blankTitle_HasViolation() {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
    }

    @Test
    void titleTooLong_HasViolation() {
        // Given
        String longTitle = "a".repeat(256);
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title(longTitle)
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
    }

    @Test
    void nullModuleType_HasViolation() {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("Test Template")
                .description("Test Description")
                .moduleType(null)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("moduleType");
    }

    @Test
    void nullFrequency_HasViolation() {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("Test Template")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(null)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("frequency");
    }

    @Test
    void descriptionTooLong_HasViolation() {
        // Given
        String longDescription = "a".repeat(1001);
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("Test Template")
                .description(longDescription)
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");
    }

    @Test
    void nullDescription_NoViolation() {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("Test Template")
                .description(null)
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When
        Set<ConstraintViolation<ChecklistTemplateCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }
}
