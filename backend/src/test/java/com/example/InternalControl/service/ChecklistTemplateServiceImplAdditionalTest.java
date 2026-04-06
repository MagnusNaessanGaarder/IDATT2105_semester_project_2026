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
 * Additional unit tests for ChecklistTemplateServiceImpl.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ChecklistTemplateServiceImplAdditionalTest {

    @Mock
    private ChecklistTemplateRepository templateRepository;

    @Mock
    private OrganizationRepository orgRepository;

    @InjectMocks
    private ChecklistTemplateServiceImpl templateService;

    @Test
    void shouldCreateTemplateForAlcoholModule() {
        // Given
        Integer orgNumber = 123;
        Long userId = 1L;

        ChecklistTemplate template = new ChecklistTemplate();
        template.setTitle("Alcohol Check");
        template.setModuleType(ModuleType.ALCOHOL);
        template.setFrequency(Frequency.WEEKLY);

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

        when(orgRepository.existsById(orgNumber)).thenReturn(true);
        when(templateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> {
            ChecklistTemplate saved = inv.getArgument(0);
            saved.setTemplateId(1L);
            return saved;
        });

        // Test all frequencies
        for (Frequency frequency : Frequency.values()) {
            ChecklistTemplate template = new ChecklistTemplate();
            template.setTitle("Test " + frequency);
            template.setModuleType(ModuleType.FOOD);
            template.setFrequency(frequency);

            ChecklistTemplate result = templateService.createTemplate(template, orgNumber, userId);
            assertThat(result.getFrequency()).isEqualTo(frequency);
        }
    }

    @Test
    void shouldUpdateTemplateWithPartialData() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate existing = new ChecklistTemplate();
        existing.setTemplateId(templateId);
        existing.setOrgNumber(orgNumber);
        existing.setTitle("Old Title");
        existing.setDescription("Old Description");
        existing.setModuleType(ModuleType.FOOD);
        existing.setFrequency(Frequency.DAILY);
        existing.setIsActive(true);

        ChecklistTemplate update = new ChecklistTemplate();
        update.setTitle("New Title");
        // Only updating title, other fields null

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
    void shouldDeleteAlreadyInactiveTemplate() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate template = new ChecklistTemplate();
        template.setTemplateId(templateId);
        template.setOrgNumber(orgNumber);
        template.setIsActive(false); // Already inactive

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));

        // When
        templateService.deleteTemplate(templateId, orgNumber);

        // Then
        assertThat(template.getIsActive()).isFalse();
        verify(templateRepository).save(template);
    }

    @Test
    void shouldGetTemplatesByModuleAlcohol() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate template1 = new ChecklistTemplate();
        template1.setTemplateId(1L);
        template1.setOrgNumber(orgNumber);
        template1.setModuleType(ModuleType.ALCOHOL);

        ChecklistTemplate template2 = new ChecklistTemplate();
        template2.setTemplateId(2L);
        template2.setOrgNumber(orgNumber);
        template2.setModuleType(ModuleType.ALCOHOL);

        when(templateRepository.findByOrgNumberAndModuleType(orgNumber, ModuleType.ALCOHOL))
                .thenReturn(Arrays.asList(template1, template2));

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByModule(orgNumber, ModuleType.ALCOHOL);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getModuleType()).isEqualTo(ModuleType.ALCOHOL);
        assertThat(result.get(1).getModuleType()).isEqualTo(ModuleType.ALCOHOL);
    }

    @Test
    void shouldGetTemplatesByModuleFood() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate template = new ChecklistTemplate();
        template.setTemplateId(1L);
        template.setOrgNumber(orgNumber);
        template.setModuleType(ModuleType.FOOD);

        when(templateRepository.findByOrgNumberAndModuleType(orgNumber, ModuleType.FOOD))
                .thenReturn(Arrays.asList(template));

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByModule(orgNumber, ModuleType.FOOD);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModuleType()).isEqualTo(ModuleType.FOOD);
    }

    @Test
    void shouldReturnEmptyListWhenNoTemplatesForModule() {
        // Given
        Integer orgNumber = 123;

        when(templateRepository.findByOrgNumberAndModuleType(orgNumber, ModuleType.FOOD))
                .thenReturn(Collections.emptyList());

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByModule(orgNumber, ModuleType.FOOD);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetActiveTemplatesWhenAllAreActive() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate template1 = new ChecklistTemplate();
        template1.setTemplateId(1L);
        template1.setIsActive(true);

        ChecklistTemplate template2 = new ChecklistTemplate();
        template2.setTemplateId(2L);
        template2.setIsActive(true);

        when(templateRepository.findByOrgNumberAndIsActiveTrue(orgNumber))
                .thenReturn(Arrays.asList(template1, template2));

        // When
        List<ChecklistTemplate> result = templateService.getActiveTemplates(orgNumber);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIsActive()).isTrue();
        assertThat(result.get(1).getIsActive()).isTrue();
    }

    @Test
    void shouldGetActiveTemplatesWhenSomeAreInactive() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate activeTemplate = new ChecklistTemplate();
        activeTemplate.setTemplateId(1L);
        activeTemplate.setIsActive(true);

        // Repository should only return active ones
        when(templateRepository.findByOrgNumberAndIsActiveTrue(orgNumber))
                .thenReturn(Arrays.asList(activeTemplate));

        // When
        List<ChecklistTemplate> result = templateService.getActiveTemplates(orgNumber);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void shouldGetTemplateWithAllFields() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate template = new ChecklistTemplate();
        template.setTemplateId(templateId);
        template.setOrgNumber(orgNumber);
        template.setTitle("Complete Template");
        template.setDescription("Full Description");
        template.setModuleType(ModuleType.FOOD);
        template.setFrequency(Frequency.DAILY);
        template.setIsActive(true);

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));

        // When
        ChecklistTemplate result = templateService.getTemplate(templateId, orgNumber);

        // Then
        assertThat(result.getTemplateId()).isEqualTo(templateId);
        assertThat(result.getTitle()).isEqualTo("Complete Template");
        assertThat(result.getDescription()).isEqualTo("Full Description");
        assertThat(result.getModuleType()).isEqualTo(ModuleType.FOOD);
        assertThat(result.getFrequency()).isEqualTo(Frequency.DAILY);
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void shouldGetTemplatesByOrgWhenManyExist() {
        // Given
        Integer orgNumber = 123;

        ChecklistTemplate template1 = new ChecklistTemplate();
        template1.setTemplateId(1L);
        template1.setOrgNumber(orgNumber);
        template1.setTitle("Template 1");

        ChecklistTemplate template2 = new ChecklistTemplate();
        template2.setTemplateId(2L);
        template2.setOrgNumber(orgNumber);
        template2.setTitle("Template 2");

        ChecklistTemplate template3 = new ChecklistTemplate();
        template3.setTemplateId(3L);
        template3.setOrgNumber(orgNumber);
        template3.setTitle("Template 3");

        when(templateRepository.findByOrgNumber(orgNumber))
                .thenReturn(Arrays.asList(template1, template2, template3));

        // When
        List<ChecklistTemplate> result = templateService.getTemplatesByOrg(orgNumber);

        // Then
        assertThat(result).hasSize(3);
    }
}
