package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistRunItemUpdateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistRunItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistRunResponse;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.checklist.ChecklistRunService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 */
@RestController
@RequestMapping("/api/v1/checklists/runs")
@Tag(name = "Checklist Runs", description = "Perform and manage checklist runs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChecklistRunController {

    private final ChecklistRunService runService;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get all runs for organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved runs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<ChecklistRunResponse>> getRuns(
            @RequestParam Integer orgNumber,
            @RequestParam(required = false) RunStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
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

    @Operation(summary = "Get run by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved run"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Run not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ChecklistRunResponse> getRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(mapToResponse(run));
    }

    @Operation(summary = "Create new run from template")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Run created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistRunResponse> createRun(
            @Valid @RequestBody ChecklistRunCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
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

    @Operation(summary = "Complete a run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run completed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - run already completed or invalid state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Run not found")
    })
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ChecklistRunResponse> completeRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.completeRun(id, orgNumber);
        return ResponseEntity.ok(mapToResponse(run));
    }

    @Operation(summary = "Update run item (answer question)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Run or item not found")
    })
    @PutMapping("/{runId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ChecklistRunItemResponse> updateRunItem(
            @PathVariable Long runId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistRunItemUpdateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
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

    @Operation(summary = "Get all items for a run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved items"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Run not found")
    })
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<ChecklistRunItemResponse>> getRunItems(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(run.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList()));
    }

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

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
