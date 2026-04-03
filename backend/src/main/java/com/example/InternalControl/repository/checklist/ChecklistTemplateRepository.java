package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.shared.enums.Frequency;
import com.example.InternalControl.shared.enums.ModuleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistTemplate entity.
 * Provides CRUD operations and custom queries for checklist templates.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {

    /**
     * Find all templates by organization number (with items eagerly loaded).
     *
     * @param orgNumber the organization number
     * @return list of templates
     */
    @EntityGraph(attributePaths = {"items"})
    List<ChecklistTemplate> findByOrgNumber(Integer orgNumber);

    /**
     * Find templates by organization and module type (with items eagerly loaded).
     *
     * @param orgNumber the organization number
     * @param moduleType the module type (FOOD or ALCOHOL)
     * @return list of templates
     */
    @EntityGraph(attributePaths = {"items"})
    List<ChecklistTemplate> findByOrgNumberAndModuleType(Integer orgNumber, ModuleType moduleType);

    /**
     * Find active templates by organization (with items eagerly loaded).
     *
     * @param orgNumber the organization number
     * @return list of active templates
     */
    @EntityGraph(attributePaths = {"items"})
    List<ChecklistTemplate> findByOrgNumberAndIsActiveTrue(Integer orgNumber);

    /**
     * Find templates by organization and frequency.
     *
     * @param orgNumber the organization number
     * @param frequency the frequency (DAILY, WEEKLY, etc.)
     * @return list of templates
     */
    List<ChecklistTemplate> findByOrgNumberAndFrequency(Integer orgNumber, Frequency frequency);

    /**
     * Find template by ID and organization.
     *
     * @param templateId the template ID
     * @param orgNumber the organization number
     * @return optional template
     */
    Optional<ChecklistTemplate> findByTemplateIdAndOrgNumber(Long templateId, Integer orgNumber);

    /**
     * Check if template exists for organization.
     *
     * @param templateId the template ID
     * @param orgNumber the organization number
     * @return true if exists
     */
    boolean existsByTemplateIdAndOrgNumber(Long templateId, Integer orgNumber);

    /**
     * Find all active templates.
     * Used by scheduler for auto-generation.
     *
     * @return list of active templates
     */
    List<ChecklistTemplate> findByIsActiveTrue();
}
