package com.example.InternalControl.service;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.service.checklist.ChecklistTemplateServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChecklistTemplateServiceImpl.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ChecklistTemplateServiceImplTest {

    @Mock
    private ChecklistTemplateRepository templateRepository;

    @Mock
    private OrganizationRepository orgRepository;

    @InjectMocks
    private ChecklistTemplateServiceImpl templateService;

    @Test
    void shouldCreateTemplate() {
        // Given
        Integer orgNumber = 123;
        Long userId = 1L;

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title("Daily Temperature Check")
                .description("Check all fridge temperatures")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        when(orgRepository.existsById(orgNumber)).thenReturn(true);
        when(templateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> {
            ChecklistTemplate saved = inv.getArgument(0);
            saved.setTemplateId(1L);
            return saved;
        });

        // When
        ChecklistTemplate result = templateService.createTemplate(template, orgNumber, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTemplateId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Daily Temperature Check");
        assertThat(result.getOrgNumber()).isEqualTo(orgNumber);
        assertThat(result.getCreatedByUserId()).isEqualTo(userId);
        assertThat(result.getIsActive()).isTrue();

        verify(templateRepository).save(any(ChecklistTemplate.class));
    }

    @Test
    void shouldThrowWhenOrgNotFound() {
        // Given
        Integer orgNumber = 999;
        Long userId = 1L;

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title("Test")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        when(orgRepository.existsById(orgNumber)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> templateService.createTemplate(template, orgNumber, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Organization not found: " + orgNumber);

        verify(templateRepository, never()).save(any());
    }

    @Test
    void shouldGetTemplate() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .title("Test Template")
                .build();

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));

        // When
        ChecklistTemplate result = templateService.getTemplate(templateId, orgNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTemplateId()).isEqualTo(templateId);
        assertThat(result.getTitle()).isEqualTo("Test Template");
    }

    @Test
    void shouldThrowWhenTemplateNotFound() {
        // Given
        Long templateId = 999L;
        Integer orgNumber = 123;

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> templateService.getTemplate(templateId, orgNumber))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Checklist template not found: " + templateId);
    }

    @Test
    void shouldGetTemplatesByOrg() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate template1 = ChecklistTemplate.builder()
                .templateId(1L)
                .orgNumber(orgNumber)
                .title("Template 1")
                .build();

        ChecklistTemplate template2 = ChecklistTemplate.builder()
                .templateId(2L)
                .orgNumber(orgNumber)
                .title("Template 2")
                .build();

        when(templateRepository.findByOrgNumber(orgNumber))
                .thenReturn(Arrays.asList(template1, template2));

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByOrg(orgNumber);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Template 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Template 2");
    }

    @Test
    void shouldReturnEmptyListWhenNoTemplates() {
        // Given
        Integer orgNumber = 123;

        when(templateRepository.findByOrgNumber(orgNumber))
                .thenReturn(Collections.emptyList());

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByOrg(orgNumber);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdateTemplate() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate existing = ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .title("Old Title")
                .description("Old Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        ChecklistTemplate update = ChecklistTemplate.builder()
                .title("New Title")
                .description("New Description")
                .build();

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(existing));
        when(templateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChecklistTemplate result = templateService.updateTemplate(templateId, update, orgNumber);

        // Then
        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("New Description");
    }

    @Test
    void shouldUpdateTemplateWithPartialData() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate existing = ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .title("Old Title")
                .description("Old Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // Only update title, keep other fields
        ChecklistTemplate update = ChecklistTemplate.builder()
                .title("New Title")
                .build();

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(existing));
        when(templateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChecklistTemplate result = templateService.updateTemplate(templateId, update, orgNumber);

        // Then
        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("Old Description"); // Should remain unchanged
        assertThat(result.getModuleType()).isEqualTo(ModuleType.FOOD); // Should remain unchanged
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentTemplate() {
        // Given
        Long templateId = 999L;
        Integer orgNumber = 123;

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.empty());

        ChecklistTemplate update = ChecklistTemplate.builder()
                .title("New Title")
                .build();

        // When/Then
        assertThatThrownBy(() -> templateService.updateTemplate(templateId, update, orgNumber))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Checklist template not found");
    }

    @Test
    void shouldDeleteTemplate() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .isActive(true)
                .build();

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));

        // When
        templateService.deleteTemplate(templateId, orgNumber);

        // Then
        assertThat(template.getIsActive()).isFalse();
        verify(templateRepository).save(template);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentTemplate() {
        // Given
        Long templateId = 999L;
        Integer orgNumber = 123;

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> templateService.deleteTemplate(templateId, orgNumber))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Checklist template not found");
    }

    @Test
    void shouldGetActiveTemplates() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate active = ChecklistTemplate.builder()
                .templateId(1L)
                .orgNumber(orgNumber)
                .isActive(true)
                .build();

        when(templateRepository.findByOrgNumberAndIsActiveTrue(orgNumber))
                .thenReturn(Arrays.asList(active));

        // When
        List<ChecklistTemplate> result = templateService.getActiveTemplates(orgNumber);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void shouldGetTemplatesByModule() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate foodTemplate = ChecklistTemplate.builder()
                .templateId(1L)
                .orgNumber(orgNumber)
                .moduleType(ModuleType.FOOD)
                .build();

        ChecklistTemplate alcoholTemplate = ChecklistTemplate.builder()
                .templateId(2L)
                .orgNumber(orgNumber)
                .moduleType(ModuleType.ALCOHOL)
                .build();

        when(templateRepository.findByOrgNumberAndModuleType(orgNumber, ModuleType.FOOD))
                .thenReturn(Arrays.asList(foodTemplate));

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByModule(orgNumber, ModuleType.FOOD);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModuleType()).isEqualTo(ModuleType.FOOD);
    }

    @Test
    void shouldCreateTemplateForAlcoholModule() {
        // Given
        Integer orgNumber = 123;
        Long userId = 1L;

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title("Alcohol Inventory Check")
                .description("Verify alcohol stock levels")
                .moduleType(ModuleType.ALCOHOL)
                .frequency(Frequency.WEEKLY)
                .build();

        when(orgRepository.existsById(orgNumber)).thenReturn(true);
        when(templateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> {
            ChecklistTemplate saved = inv.getArgument(0);
            saved.setTemplateId(1L);
            return saved;
        });

        // When
        ChecklistTemplate result = templateService.createTemplate(template, orgNumber, userId);

        // Then
        assertThat(result.getModuleType()).isEqualTo(ModuleType.ALCOHOL);
        assertThat(result.getFrequency()).isEqualTo(Frequency.WEEKLY);
    }

    @Test
    void shouldCreateTemplateWithAllFrequencies() {
        // Given
        Integer orgNumber = 123;
        Long userId = 1L;

        for (Frequency frequency : Frequency.values()) {
            ChecklistTemplate template = ChecklistTemplate.builder()
                    .title("Test " + frequency)
                    .moduleType(ModuleType.FOOD)
                    .frequency(frequency)
                    .build();

            when(orgRepository.existsById(orgNumber)).thenReturn(true);
            when(templateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> {
                ChecklistTemplate saved = inv.getArgument(0);
                saved.setTemplateId(1L);
                return saved;
            });

            // When
            ChecklistTemplate result = templateService.createTemplate(template, orgNumber, userId);

            // Then
            assertThat(result.getFrequency()).isEqualTo(frequency);
        }
    }
}
