package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of ChecklistTemplateService.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChecklistTemplateServiceImpl implements ChecklistTemplateService {

  private final ChecklistTemplateRepository templateRepository;

  private final OrganizationRepository orgRepository;

  @Override
  public ChecklistTemplate createTemplate(ChecklistTemplate template, Integer orgNumber, Long userId) {
    validateOrganizationExists(orgNumber);

    template.setOrgNumber(orgNumber);
    template.setCreatedByUserId(userId);
    template.setIsActive(true);

    return templateRepository.save(template);
  }

  @Override
  public ChecklistTemplate updateTemplate(Long templateId, ChecklistTemplate template, Integer orgNumber) {
    ChecklistTemplate existing = findTemplateByIdAndOrg(templateId, orgNumber);

    if (template.getTitle() != null) {
      existing.setTitle(template.getTitle());
    }
    if (template.getDescription() != null) {
      existing.setDescription(template.getDescription());
    }
    if (template.getModuleType() != null) {
      existing.setModuleType(template.getModuleType());
    }
    if (template.getFrequency() != null) {
      existing.setFrequency(template.getFrequency());
    }
    if (template.getItems() != null && !template.getItems().isEmpty()) {
      Map<Integer, ChecklistTemplateItem> existingItemsBySortOrder = existing.getItems().stream()
          .collect(Collectors.toMap(ChecklistTemplateItem::getSortOrder, Function.identity(), (left, right) -> left));

      for (ChecklistTemplateItem item : template.getItems()) {
        ChecklistTemplateItem target = existingItemsBySortOrder.get(item.getSortOrder());

        if (target == null) {
          target = ChecklistTemplateItem.builder()
              .sortOrder(item.getSortOrder())
              .build();
          existing.addItem(target);
        }

        target.setSortOrder(item.getSortOrder());
        target.setLabel(item.getLabel());
        target.setDescription(item.getDescription());
        target.setItemType(item.getItemType());
        target.setIsRequired(item.getIsRequired());
        target.setExpectedText(item.getExpectedText());
        target.setExpectedNumericMin(item.getExpectedNumericMin());
        target.setExpectedNumericMax(item.getExpectedNumericMax());
        target.setChoiceOptionsJson(item.getChoiceOptionsJson());
      }
    }

    return templateRepository.save(existing);
  }

  @Override
  public void deleteTemplate(Long templateId, Integer orgNumber) {
    ChecklistTemplate template = findTemplateByIdAndOrg(templateId, orgNumber);
    template.deactivate();
    templateRepository.save(template);
  }

  @Override
  @Transactional(readOnly = true)
  public ChecklistTemplate getTemplate(Long templateId, Integer orgNumber) {
    return findTemplateByIdAndOrg(templateId, orgNumber);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChecklistTemplate> getTemplatesByOrg(Integer orgNumber) {
    return templateRepository.findByOrgNumber(orgNumber);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChecklistTemplate> getTemplatesByModule(Integer orgNumber, ModuleType moduleType) {
    return templateRepository.findByOrgNumberAndModuleType(orgNumber, moduleType);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChecklistTemplate> getActiveTemplates(Integer orgNumber) {
    return templateRepository.findByOrgNumberAndIsActiveTrue(orgNumber);
  }

  private ChecklistTemplate findTemplateByIdAndOrg(Long templateId, Integer orgNumber) {
    return templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber)
        .orElseThrow(() -> new EntityNotFoundException(
            "Checklist template not found: " + templateId));
  }

  private void validateOrganizationExists(Integer orgNumber) {
    if (!orgRepository.existsById(orgNumber)) {
      throw new EntityNotFoundException("Organization not found: " + orgNumber);
    }
  }
}
