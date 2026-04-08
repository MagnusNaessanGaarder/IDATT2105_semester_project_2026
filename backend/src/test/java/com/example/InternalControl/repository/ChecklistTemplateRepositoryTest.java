package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ChecklistTemplateRepository.
 * Tests template queries with module type and frequency filtering.
 */
@SpringBootTest
@DisplayName("ChecklistTemplateRepository Integration Tests")
class ChecklistTemplateRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ChecklistTemplateRepository checklistTemplateRepository;

    private static final Integer ORG_NUMBER = 937219997;

    @Test
    @DisplayName("Should find templates by org number")
    void shouldFindTemplatesByOrgNumber() {
        // Given
        ChecklistTemplate template1 = createTemplate("Template 1", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate template2 = createTemplate("Template 2", ModuleType.ALCOHOL, Frequency.WEEKLY, true);
        checklistTemplateRepository.save(template1);
        checklistTemplateRepository.save(template2);

        // When
        List<ChecklistTemplate> templates = checklistTemplateRepository.findByOrgNumber(ORG_NUMBER);

        // Then
        assertThat(templates).hasSize(2);
        assertThat(templates).extracting(ChecklistTemplate::getTitle).contains("Template 1", "Template 2");
    }

    @Test
    @DisplayName("Should find templates by org number and module type")
    void shouldFindTemplatesByOrgNumberAndModuleType() {
        // Given
        ChecklistTemplate foodTemplate = createTemplate("Food Template", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate alcoholTemplate = createTemplate("Alcohol Template", ModuleType.ALCOHOL, Frequency.DAILY, true);
        checklistTemplateRepository.save(foodTemplate);
        checklistTemplateRepository.save(alcoholTemplate);

        // When
        List<ChecklistTemplate> foodTemplates = checklistTemplateRepository.findByOrgNumberAndModuleType(ORG_NUMBER, ModuleType.FOOD);

        // Then
        assertThat(foodTemplates).hasSize(1);
        assertThat(foodTemplates.get(0).getTitle()).isEqualTo("Food Template");
        assertThat(foodTemplates.get(0).getModuleType()).isEqualTo(ModuleType.FOOD);
    }

    @Test
    @DisplayName("Should find active templates by org number")
    void shouldFindActiveTemplatesByOrgNumber() {
        // Given
        ChecklistTemplate activeTemplate = createTemplate("Active Template", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate inactiveTemplate = createTemplate("Inactive Template", ModuleType.FOOD, Frequency.DAILY, false);
        checklistTemplateRepository.save(activeTemplate);
        checklistTemplateRepository.save(inactiveTemplate);

        // When
        List<ChecklistTemplate> activeTemplates = checklistTemplateRepository.findByOrgNumberAndIsActiveTrue(ORG_NUMBER);

        // Then
        assertThat(activeTemplates).hasSize(1);
        assertThat(activeTemplates.get(0).getTitle()).isEqualTo("Active Template");
        assertThat(activeTemplates.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should find templates by org number and frequency")
    void shouldFindTemplatesByOrgNumberAndFrequency() {
        // Given
        ChecklistTemplate dailyTemplate = createTemplate("Daily Template", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate weeklyTemplate = createTemplate("Weekly Template", ModuleType.FOOD, Frequency.WEEKLY, true);
        ChecklistTemplate monthlyTemplate = createTemplate("Monthly Template", ModuleType.FOOD, Frequency.MONTHLY, true);
        checklistTemplateRepository.save(dailyTemplate);
        checklistTemplateRepository.save(weeklyTemplate);
        checklistTemplateRepository.save(monthlyTemplate);

        // When
        List<ChecklistTemplate> dailyTemplates = checklistTemplateRepository.findByOrgNumberAndFrequency(ORG_NUMBER, Frequency.DAILY);

        // Then
        assertThat(dailyTemplates).hasSize(1);
        assertThat(dailyTemplates.get(0).getTitle()).isEqualTo("Daily Template");
        assertThat(dailyTemplates.get(0).getFrequency()).isEqualTo(Frequency.DAILY);
    }

    @Test
    @DisplayName("Should find template by ID and org number")
    void shouldFindTemplateByIdAndOrgNumber() {
        // Given
        ChecklistTemplate template = createTemplate("Specific Template", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // When
        Optional<ChecklistTemplate> found = checklistTemplateRepository.findByTemplateIdAndOrgNumber(saved.getTemplateId(), ORG_NUMBER);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Specific Template");
    }

    @Test
    @DisplayName("Should check if template exists by ID and org number")
    void shouldCheckIfTemplateExistsByIdAndOrgNumber() {
        // Given
        ChecklistTemplate template = createTemplate("Existing Template", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // When & Then
        assertThat(checklistTemplateRepository.existsByTemplateIdAndOrgNumber(saved.getTemplateId(), ORG_NUMBER)).isTrue();
        assertThat(checklistTemplateRepository.existsByTemplateIdAndOrgNumber(99999L, ORG_NUMBER)).isFalse();
    }

    @Test
    @DisplayName("Should find all active templates")
    void shouldFindAllActiveTemplates() {
        // Given - create templates for different orgs
        ChecklistTemplate org1Active = createTemplate("Org1 Active", ModuleType.FOOD, Frequency.DAILY, true);
        org1Active.setOrgNumber(ORG_NUMBER);

        ChecklistTemplate org2Active = createTemplate("Org2 Active", ModuleType.ALCOHOL, Frequency.WEEKLY, true);
        org2Active.setOrgNumber(123456789);

        ChecklistTemplate inactive = createTemplate("Inactive", ModuleType.FOOD, Frequency.DAILY, false);
        inactive.setOrgNumber(ORG_NUMBER);

        checklistTemplateRepository.save(org1Active);
        checklistTemplateRepository.save(org2Active);
        checklistTemplateRepository.save(inactive);

        // When
        List<ChecklistTemplate> allActiveTemplates = checklistTemplateRepository.findByIsActiveTrue();

        // Then
        assertThat(allActiveTemplates).hasSize(2);
        assertThat(allActiveTemplates).extracting(ChecklistTemplate::getTitle).contains("Org1 Active", "Org2 Active");
    }

    @Test
    @DisplayName("Should return empty list for non-existent org number")
    void shouldReturnEmptyListForNonExistentOrgNumber() {
        // When
        List<ChecklistTemplate> templates = checklistTemplateRepository.findByOrgNumber(999999999);

        // Then
        assertThat(templates).isEmpty();
    }

    @Test
    @DisplayName("Should return empty optional for template in different org")
    void shouldReturnEmptyOptionalForTemplateInDifferentOrg() {
        // Given
        ChecklistTemplate template = createTemplate("Template", ModuleType.FOOD, Frequency.DAILY, true);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // When
        Optional<ChecklistTemplate> found = checklistTemplateRepository.findByTemplateIdAndOrgNumber(saved.getTemplateId(), 123456789);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should filter templates by module type correctly")
    void shouldFilterTemplatesByModuleTypeCorrectly() {
        // Given - create multiple FOOD templates and one ALCOHOL template
        checklistTemplateRepository.save(createTemplate("Food 1", ModuleType.FOOD, Frequency.DAILY, true));
        checklistTemplateRepository.save(createTemplate("Food 2", ModuleType.FOOD, Frequency.WEEKLY, true));
        checklistTemplateRepository.save(createTemplate("Food 3", ModuleType.FOOD, Frequency.MONTHLY, true));
        checklistTemplateRepository.save(createTemplate("Alcohol 1", ModuleType.ALCOHOL, Frequency.DAILY, true));

        // When
        List<ChecklistTemplate> foodTemplates = checklistTemplateRepository.findByOrgNumberAndModuleType(ORG_NUMBER, ModuleType.FOOD);
        List<ChecklistTemplate> alcoholTemplates = checklistTemplateRepository.findByOrgNumberAndModuleType(ORG_NUMBER, ModuleType.ALCOHOL);

        // Then
        assertThat(foodTemplates).hasSize(3);
        assertThat(alcoholTemplates).hasSize(1);
    }

    private ChecklistTemplate createTemplate(String title, ModuleType moduleType, Frequency frequency, boolean isActive) {
        return ChecklistTemplate.builder()
                .orgNumber(ORG_NUMBER)
                .moduleType(moduleType)
                .title(title)
                .description("Description for " + title)
                .frequency(frequency)
                .isActive(isActive)
                .createdByUserId(1L)
                .build();
    }
}
