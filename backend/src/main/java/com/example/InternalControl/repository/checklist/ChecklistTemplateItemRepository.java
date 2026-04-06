package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistTemplateItem entity.
 * Provides CRUD operations for checklist template items (questions).
 */
@Repository
public interface ChecklistTemplateItemRepository extends JpaRepository<ChecklistTemplateItem, Long> {

    List<ChecklistTemplateItem> findByTemplateTemplateIdOrderBySortOrderAsc(Long templateId);

    Optional<ChecklistTemplateItem> findByItemIdAndTemplateTemplateId(Long itemId, Long templateId);

    long countByTemplateTemplateId(Long templateId);

    void deleteByTemplateTemplateId(Long templateId);
}
