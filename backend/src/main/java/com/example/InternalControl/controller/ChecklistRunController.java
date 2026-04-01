package com.example.InternalControl.controller;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistRunItemUpdateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistRunItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistRunResponse;
import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.ChecklistRunItem;
import com.example.InternalControl.model.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.ChecklistRunService;
import com.example.InternalControl.service.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Checklist Run operations.
 * Users perform checklists through these endpoints.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/checklists/runs")
@Tag(name = "Checklist Runs", description = "Perform and manage checklist runs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChecklistRunController {

    /**
     * Length of the "Bearer " prefix in Authorization header.
     */
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final ChecklistRunService runService;

    private final JwtService jwtService;

    private final UserOrganizationService userOrgService;

    /**
     * Get all runs for organization.
     *
     * @param orgNumber the organization number
     * @param status optional status filter
     * @param request the HTTP request
     * @return list of runs
     */
    @Operation(summary = "Get all runs for organization")
    @GetMapping
    public ResponseEntity<List<ChecklistRunResponse>> getRuns(
            @RequestParam Integer orgNumber,
            @RequestParam(required = false) RunStatus status,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);

        List<ChecklistRun> runs;
        if (status != null) {
            runs = runService.getRunsByStatus(orgNumber, status);
        } else {
            runs = runService.getRunsByOrg(orgNumber);
        }

        return ResponseEntity.ok(runs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Get run by ID.
     *
     * @param id the run ID
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return the run
     */
    @Operation(summary = "Get run by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ChecklistRunResponse> getRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(mapToResponse(run));
    }

    /**
     * Create new run from template.
     *
     * @param requestDto the create request
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return the created run
     */
    @Operation(summary = "Create new run from template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistRunResponse> createRun(
            @Valid @RequestBody ChecklistRunCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.createRun(
                requestDto.getTemplateId(),
                orgNumber,
                userId,
                requestDto.getRunDate()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(run.getRunId())
                .toUri();

        return ResponseEntity.created(location).body(mapToResponse(run));
    }

    /**
     * Complete a run.
     *
     * @param id the run ID
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return the completed run
     */
    @Operation(summary = "Complete a run")
    @PutMapping("/{id}/complete")
    public ResponseEntity<ChecklistRunResponse> completeRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.completeRun(id, orgNumber);
        return ResponseEntity.ok(mapToResponse(run));
    }

    /**
     * Update run item (answer question).
     *
     * @param runId the run ID
     * @param itemId the item ID
     * @param requestDto the update request
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return the updated item
     */
    @Operation(summary = "Update run item (answer question)")
    @PutMapping("/{runId}/items/{itemId}")
    public ResponseEntity<ChecklistRunItemResponse> updateRunItem(
            @PathVariable Long runId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistRunItemUpdateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRunItem item = ChecklistRunItem.builder()
                .booleanValue(requestDto.getBooleanValue())
                .textValue(requestDto.getTextValue())
                .numericValue(requestDto.getNumericValue())
                .selectedChoice(requestDto.getSelectedChoice())
                .isDeviation(requestDto.getIsDeviation())
                .commentText(requestDto.getCommentText())
                .build();

        ChecklistRunItem updated = runService.updateRunItem(runId, itemId, item, orgNumber);
        return ResponseEntity.ok(mapToItemResponse(updated));
    }

    /**
     * Get all items for a run.
     *
     * @param id the run ID
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return list of items
     */
    @Operation(summary = "Get all items for a run")
    @GetMapping("/{id}/items")
    public ResponseEntity<List<ChecklistRunItemResponse>> getRunItems(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(run.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Map ChecklistRun entity to response DTO.
     *
     * @param run the run entity
     * @return the response DTO
     */
    private ChecklistRunResponse mapToResponse(ChecklistRun run) {
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
                .items(run.getItems() != null ? run.getItems().stream()
                        .map(this::mapToItemResponse)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    /**
     * Map ChecklistRunItem entity to response DTO.
     *
     * @param item the item entity
     * @return the response DTO
     */
    private ChecklistRunItemResponse mapToItemResponse(ChecklistRunItem item) {
        String templateItemLabel = null;
        if (item.getRun() != null && item.getRun().getTemplate() != null) {
            templateItemLabel = item.getRun().getTemplate().getItems().stream()
                    .filter(templateItem -> templateItem.getItemId().equals(item.getTemplateItemId()))
                    .findFirst()
                    .map(ChecklistTemplateItem::getLabel)
                    .orElse(null);
        }

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

    /**
     * Extract user ID from JWT token in the Authorization header.
     *
     * @param request the HTTP request
     * @return the user ID
     */
    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        Long userId = jwtService.extractUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("User ID not found in token");
        }
        return userId;
    }

    /**
     * Validate that user has access to the organization.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     */
    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
