package com.example.InternalControl.repository;

import com.example.InternalControl.model.ChecklistTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistTemplateItem entity.
 * Provides CRUD operations for checklist template items (questions).
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface ChecklistTemplateItemRepository extends JpaRepository<ChecklistTemplateItem, Long> {

    /**
     * Find all items for a template, ordered by sort order.
     *
     * @param templateId the template ID
     * @return list of items in order
     */
    List<ChecklistTemplateItem> findByTemplateTemplateIdOrderBySortOrderAsc(Long templateId);

    /**
     * Find item by ID and template ID.
     *
     * @param itemId the item ID
     * @param templateId the template ID
     * @return optional item
     */
    Optional<ChecklistTemplateItem> findByItemIdAndTemplateTemplateId(Long itemId, Long templateId);

    /**
     * Count items for a template.
     *
     * @param templateId the template ID
     * @return count of items
     */
    long countByTemplateTemplateId(Long templateId);

    /**
     * Delete all items for a template.
     *
     * @param templateId the template ID
     */
    void deleteByTemplateTemplateId(Long templateId);
}
