package com.example.InternalControl.service.checklist.mapper;

import com.example.InternalControl.dto.checklist.response.ChecklistRunItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistRunResponse;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper for converting ChecklistRun entities to DTOs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class ChecklistRunMapper {

    public ChecklistRunResponse toResponse(ChecklistRun run) {
        if (run == null) {
            return null;
        }

        // Precompute template item labels for O(1) lookup
        Map<Long, String> templateItemLabelMap = buildTemplateItemLabelMap(run);

        return ChecklistRunResponse.builder()
                .runId(run.getRunId())
                .templateId(run.getTemplate() != null ? run.getTemplate().getTemplateId() : null)
                .templateTitle(run.getTemplate() != null ? run.getTemplate().getTitle() : null)
                .orgNumber(run.getOrgNumber())
                .locationId(run.getLocationId())
                .performedByUserId(run.getPerformedByUserId())
                .assignedToUserId(run.getAssignedToUserId())
                .runDate(run.getRunDate())
                .dueAt(run.getDueAt())
                .completedAt(run.getCompletedAt())
                .status(run.getStatus())
                .notes(run.getNotes())
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt())
                .items(run.getItems() != null
                        ? run.getItems().stream()
                                .map(item -> toItemResponse(item, templateItemLabelMap))
                                .collect(Collectors.toList())
                        : null)
                .build();
    }

    /**
     * Builds a map of template item IDs to labels for O(1) lookup.
     * Prevents O(n²) complexity when mapping multiple items.
     */
    private Map<Long, String> buildTemplateItemLabelMap(ChecklistRun run) {
        if (run.getTemplate() == null || run.getTemplate().getItems() == null) {
            return Collections.emptyMap();
        }
        return run.getTemplate().getItems().stream()
                .collect(Collectors.toMap(
                        ChecklistTemplateItem::getItemId,
                        ChecklistTemplateItem::getLabel,
                        (existing, replacement) -> existing
                ));
    }

    public ChecklistRunItemResponse toItemResponse(ChecklistRunItem item) {
        return toItemResponse(item, Collections.emptyMap());
    }

    public ChecklistRunItemResponse toItemResponse(ChecklistRunItem item, Map<Long, String> templateItemLabelMap) {
        if (item == null) {
            return null;
        }

        String templateItemLabel = templateItemLabelMap.getOrDefault(item.getTemplateItemId(), null);

        return ChecklistRunItemResponse.builder()
                .runItemId(item.getRunItemId())
                .runId(item.getRun() != null ? item.getRun().getRunId() : null)
                .templateItemId(item.getTemplateItemId())
                .templateItemLabel(templateItemLabel)
                .booleanValue(item.getBooleanValue())
                .textValue(item.getTextValue())
                .numericValue(item.getNumericValue())
                .selectedChoice(item.getSelectedChoice())
                .isDeviation(item.getIsDeviation())
                .commentText(item.getCommentText())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .hasAnswer(item.hasAnswer())
                .build();
    }

    public List<ChecklistRunResponse> toResponseList(List<ChecklistRun> runs) {
        if (runs == null) {
            return Collections.emptyList();
        }
        return runs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
