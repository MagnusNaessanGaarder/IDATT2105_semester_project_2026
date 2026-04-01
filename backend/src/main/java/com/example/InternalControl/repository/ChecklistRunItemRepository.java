package com.example.InternalControl.repository;

import com.example.InternalControl.model.ChecklistRunItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistRunItem entity.
 * Provides CRUD operations for checklist run items (answers).
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface ChecklistRunItemRepository extends JpaRepository<ChecklistRunItem, Long> {

    /**
     * Find all items for a run.
     *
     * @param runId the run ID
     * @return list of items
     */
    List<ChecklistRunItem> findByRunRunId(Long runId);

    /**
     * Find item by run and template item.
     *
     * @param runId the run ID
     * @param templateItemId the template item ID
     * @return optional item
     */
    Optional<ChecklistRunItem> findByRunRunIdAndTemplateItemId(Long runId, Long templateItemId);

    /**
     * Find items with deviations.
     *
     * @param runId the run ID
     * @return list of deviation items
     */
    List<ChecklistRunItem> findByRunRunIdAndIsDeviationTrue(Long runId);

    /**
     * Count deviations for a run.
     *
     * @param runId the run ID
     * @return count of deviations
     */
    long countByRunRunIdAndIsDeviationTrue(Long runId);

    /**
     * Check if all items are answered.
     *
     * @param runId the run ID
     * @return count of unanswered items
     */
    long countByRunRunIdAndBooleanValueIsNullAndTextValueIsNullAndNumericValueIsNullAndSelectedChoiceIsNull(
            Long runId);
}
