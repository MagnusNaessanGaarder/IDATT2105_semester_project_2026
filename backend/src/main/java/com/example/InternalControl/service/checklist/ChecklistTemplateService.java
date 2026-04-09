package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;

import java.util.List;

/**
 * Service interface for managing checklist templates.
 * <p>
 * Checklist templates define reusable structures for daily, weekly, or monthly
 * compliance checks in both IK-MAT (food safety) and IK-ALKOHOL (alcohol service)
 * modules. Templates can be activated/deactivated and assigned to specific
 * organization locations.
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
public interface ChecklistTemplateService {

    /**
     * Creates a new checklist template for an organization.
     *
     * @param template   the template data including title, description, frequency, and items
     * @param orgNumber  the organization number (org.nr.) to associate the template with
     * @param userId     the ID of the user creating the template (for audit logging)
     * @return the created ChecklistTemplate with generated ID
     * @throws jakarta.persistence.EntityNotFoundException if the organization is not found
     * @throws IllegalArgumentException                    if template data is invalid
     */
    ChecklistTemplate createTemplate(ChecklistTemplate template, Integer orgNumber, Long userId);

    /**
     * Updates an existing checklist template.
     *
     * @param templateId the ID of the template to update
     * @param template   the updated template data
     * @param orgNumber  the organization number for access validation
     * @return the updated ChecklistTemplate
     * @throws jakarta.persistence.EntityNotFoundException if the template or organization is not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks permission
     */
    ChecklistTemplate updateTemplate(Long templateId, ChecklistTemplate template, Integer orgNumber);

    /**
     * Soft-deletes a checklist template by setting it as inactive.
     * <p>
     * Note: Templates that have associated checklist runs cannot be hard-deleted
     * to maintain audit trail. They are marked as inactive instead.
     *
     * @param templateId the ID of the template to delete
     * @param orgNumber  the organization number for access validation
     * @throws jakarta.persistence.EntityNotFoundException if the template is not found
     */
    void deleteTemplate(Long templateId, Integer orgNumber);

    /**
     * Retrieves a specific checklist template by ID.
     *
     * @param templateId the ID of the template to retrieve
     * @param orgNumber  the organization number for access validation
     * @return the ChecklistTemplate with all items and metadata
     * @throws jakarta.persistence.EntityNotFoundException if the template is not found
     *                                                     or doesn't belong to the organization
     */
    ChecklistTemplate getTemplate(Long templateId, Integer orgNumber);

    /**
     * Retrieves all checklist templates for an organization.
     *
     * @param orgNumber the organization number
     * @return list of all templates (both active and inactive) sorted by creation date
     */
    List<ChecklistTemplate> getTemplatesByOrg(Integer orgNumber);

    /**
     * Retrieves templates filtered by compliance module type.
     *
     * @param orgNumber  the organization number
     * @param moduleType the module type (MAT or ALKOHOL)
     * @return list of templates for the specified module
     */
    List<ChecklistTemplate> getTemplatesByModule(Integer orgNumber, ModuleType moduleType);

    /**
     * Retrieves only active (non-deleted) templates for an organization.
     * <p>
     * This is typically used when creating new checklist runs to ensure
     * only valid templates are available for selection.
     *
     * @param orgNumber the organization number
     * @return list of active templates
     */
    List<ChecklistTemplate> getActiveTemplates(Integer orgNumber);
}
