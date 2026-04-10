package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.repository.checklist.ChecklistRunItemRepository;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ChecklistTemplateService.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChecklistTemplateServiceImpl implements ChecklistTemplateService {

  private final ChecklistTemplateRepository templateRepository;

  private final OrganizationRepository orgRepository;

  private final ChecklistRunItemRepository runItemRepository;

  @Override
  public ChecklistTemplate createTemplate(ChecklistTemplate template, Integer orgNumber, Long userId) {
    validateOrganizationExists(orgNumber);

    template.setOrgNumber(orgNumber);
    template.setCreatedByUserId(userId);
    template.setIsActive(true);

    // Ensure bidirectional relationship for items
    if (template.getItems() != null) {
      template.getItems().forEach(item -> item.setTemplate(template));
    }

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

    // Merge items in-place to avoid FK constraint violations from orphanRemoval.
    // Run items reference template items via FK; clearing and re-adding would
    // attempt to DELETE referenced rows → DataIntegrityViolationException.
    if (template.getItems() != null) {
      List<ChecklistTemplateItem> existingItems = existing.getItems();
      List<ChecklistTemplateItem> newItems = template.getItems();
      int existingSize = existingItems.size();
      int newSize = newItems.size();
      int updateCount = Math.min(existingSize, newSize);

      // Update existing items in-place
      for (int i = 0; i < updateCount; i++) {
        ChecklistTemplateItem existingItem = existingItems.get(i);
        ChecklistTemplateItem newItem = newItems.get(i);
        existingItem.setLabel(newItem.getLabel());
        existingItem.setDescription(newItem.getDescription());
        existingItem.setItemType(newItem.getItemType());
        existingItem.setIsRequired(newItem.getIsRequired());
        existingItem.setSortOrder(newItem.getSortOrder());
        existingItem.setExpectedText(newItem.getExpectedText());
        existingItem.setExpectedNumericMin(newItem.getExpectedNumericMin());
        existingItem.setExpectedNumericMax(newItem.getExpectedNumericMax());
        existingItem.setChoiceOptionsJson(newItem.getChoiceOptionsJson());
      }

      // Add new items beyond the original count
      for (int i = existingSize; i < newSize; i++) {
        existing.addItem(newItems.get(i));
      }

      // Remove trailing items only when no run items reference them
      if (existingSize > newSize) {
        List<ChecklistTemplateItem> toRemove = new ArrayList<>(
            existingItems.subList(newSize, existingSize));
        for (ChecklistTemplateItem item : toRemove) {
          if (!runItemRepository.existsByTemplateItemId(item.getItemId())) {
            existing.removeItem(item);
          }
        }
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
