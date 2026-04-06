package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistRunItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistRunItem entity.
 * Provides CRUD operations for checklist run items (answers).
 */
@Repository
public interface ChecklistRunItemRepository extends JpaRepository<ChecklistRunItem, Long> {

    List<ChecklistRunItem> findByRunRunId(Long runId);

    Optional<ChecklistRunItem> findByRunRunIdAndTemplateItemId(Long runId, Long templateItemId);

    List<ChecklistRunItem> findByRunRunIdAndIsDeviationTrue(Long runId);

    long countByRunRunIdAndIsDeviationTrue(Long runId);

    long countByRunRunIdAndBooleanValueIsNullAndTextValueIsNullAndNumericValueIsNullAndSelectedChoiceIsNull(
            Long runId);
}
