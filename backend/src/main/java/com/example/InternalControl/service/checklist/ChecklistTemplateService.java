package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;

import java.util.List;

/**
 * Service interface for checklist template operations.
 * Manages templates used to create checklist runs for compliance verification.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface ChecklistTemplateService {

    ChecklistTemplate createTemplate(ChecklistTemplate template, Integer orgNumber, Long userId);

    ChecklistTemplate updateTemplate(Long templateId, ChecklistTemplate template, Integer orgNumber);

    void deleteTemplate(Long templateId, Integer orgNumber);

    ChecklistTemplate getTemplate(Long templateId, Integer orgNumber);

    List<ChecklistTemplate> getTemplatesByOrg(Integer orgNumber);

    List<ChecklistTemplate> getTemplatesByModule(Integer orgNumber, ModuleType moduleType);

    List<ChecklistTemplate> getActiveTemplates(Integer orgNumber);
}
