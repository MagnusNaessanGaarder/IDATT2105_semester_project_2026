package com.example.InternalControl.service;

import com.example.InternalControl.model.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.repository.ChecklistTemplateRepository;
import com.example.InternalControl.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ChecklistTemplateService.
 *
 * @author TriTacLe
 * @since 1.0
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
