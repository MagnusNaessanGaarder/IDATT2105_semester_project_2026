package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistTemplate entity.
 * Provides CRUD operations and custom queries for checklist templates.
 */
@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<ChecklistTemplate> findByOrgNumber(Integer orgNumber);

    @EntityGraph(attributePaths = {"items"})
    List<ChecklistTemplate> findByOrgNumberAndModuleType(Integer orgNumber, ModuleType moduleType);

    @EntityGraph(attributePaths = {"items"})
    List<ChecklistTemplate> findByOrgNumberAndIsActiveTrue(Integer orgNumber);

    List<ChecklistTemplate> findByOrgNumberAndFrequency(Integer orgNumber, Frequency frequency);

    @EntityGraph(attributePaths = {"items"})
    Optional<ChecklistTemplate> findByTemplateIdAndOrgNumber(Long templateId, Integer orgNumber);

    boolean existsByTemplateIdAndOrgNumber(Long templateId, Integer orgNumber);

    /**
     * Find all active templates.
     * Used by scheduler for auto-generation.
     */
    List<ChecklistTemplate> findByIsActiveTrue();
}
