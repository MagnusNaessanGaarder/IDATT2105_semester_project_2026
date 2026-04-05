package com.example.InternalControl.service;

import com.example.InternalControl.model.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;

import java.util.List;

/**
 * Service interface for checklist template operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface ChecklistTemplateService {

    /**
     * Creates a new checklist template.
     *
     * @param template the template to create
     * @param orgNumber the organization number
     * @param userId the creator user ID
     * @return the created template
     */
    ChecklistTemplate createTemplate(ChecklistTemplate template, Integer orgNumber, Long userId);

    /**
     * Updates an existing template.
     *
     * @param templateId the template ID
     * @param template the updated template data
     * @param orgNumber the organization number
     * @return the updated template
     */
    ChecklistTemplate updateTemplate(Long templateId, ChecklistTemplate template, Integer orgNumber);

    /**
     * Deletes a template (soft delete by deactivating).
     *
     * @param templateId the template ID
     * @param orgNumber the organization number
     */
    void deleteTemplate(Long templateId, Integer orgNumber);

    /**
     * Gets a template by ID.
     *
     * @param templateId the template ID
     * @param orgNumber the organization number
     * @return the template
     */
    ChecklistTemplate getTemplate(Long templateId, Integer orgNumber);

    /**
     * Gets all templates for an organization.
     *
     * @param orgNumber the organization number
     * @return list of templates
     */
    List<ChecklistTemplate> getTemplatesByOrg(Integer orgNumber);

    /**
     * Gets templates by module type.
     *
     * @param orgNumber the organization number
     * @param moduleType the module type
     * @return list of templates
     */
    List<ChecklistTemplate> getTemplatesByModule(Integer orgNumber, ModuleType moduleType);

    /**
     * Gets active templates for an organization.
     *
     * @param orgNumber the organization number
     * @return list of active templates
     */
    List<ChecklistTemplate> getActiveTemplates(Integer orgNumber);
}
