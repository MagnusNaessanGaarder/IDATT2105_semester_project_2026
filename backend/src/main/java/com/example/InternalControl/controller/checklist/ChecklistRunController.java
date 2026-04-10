package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistRunItemUpdateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistRunItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistRunResponse;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistTemplateItemRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Checklist Run operations.
 * Users perform checklists through these endpoints.
 */
@RestController
@RequestMapping({"/api/v1/checklists/runs", "/api/checklists/runs"})
@Tag(name = "Checklist Runs", description = "Perform and manage checklist runs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChecklistRunController {

    private final ChecklistRunService runService;
    private final ChecklistTemplateItemRepository templateItemRepository;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get all runs for organization")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<List<ChecklistRunResponse>> getRuns(
            @RequestParam Integer orgNumber,
            @RequestParam(required = false) RunStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
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
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<ChecklistRunResponse> getRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(mapToResponse(run));
    }

    @Operation(summary = "Create new run from template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistRunResponse> createRun(
            @Valid @RequestBody ChecklistRunCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        Long userId = resolveUserId(userDetails);
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
     * Marks a checklist run as completed.
     *
     * @param id run identifier
     * @param orgNumber organization number for access validation
     * @param userDetails authenticated user details
     * @return the updated checklist run in completed state
     */
    @Operation(summary = "Complete a run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run marked as completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Run not found"),
        @ApiResponse(responseCode = "409", description = "Run cannot be completed in its current state")
    })
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<ChecklistRunResponse> completeRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.completeRun(id, orgNumber, userId);
        return ResponseEntity.ok(mapToResponse(run));
    }

    /**
     * Reopens a completed checklist run so it becomes editable again.
     *
     * @param id run identifier
     * @param orgNumber organization number for access validation
     * @param userDetails authenticated user details
     * @return the updated checklist run in a non-completed state
     */
    @Operation(summary = "Uncomplete a run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run reopened successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Run not found"),
        @ApiResponse(responseCode = "409", description = "Run cannot be uncompleted in its current state")
    })
    @PutMapping("/{id}/uncomplete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<ChecklistRunResponse> uncompleteRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.uncompleteRun(id, orgNumber);
        return ResponseEntity.ok(mapToResponse(run));
    }

    @Operation(summary = "Update run item (answer question)")
    @PutMapping("/{runId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<ChecklistRunItemResponse> updateRunItem(
            @PathVariable Long runId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistRunItemUpdateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRunItem item = ChecklistRunItem.builder()
                .booleanValue(requestDto.getBooleanValue())
                .textValue(requestDto.getTextValue())
                .numericValue(requestDto.getNumericValue())
                .selectedChoice(requestDto.getSelectedChoice())
                .isDeviation(requestDto.getIsDeviation())
                .commentText(requestDto.getCommentText())
                .build();

        ChecklistRunItem updated = runService.updateRunItem(runId, itemId, item, orgNumber, userId);
        return ResponseEntity.ok(mapToItemResponse(updated));
    }

    @Operation(summary = "Get all items for a run")
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<List<ChecklistRunItemResponse>> getRunItems(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(run.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList()));
    }

    private ChecklistRunResponse mapToResponse(ChecklistRun run) {
        Map<Long, String> itemLabelsById;
        if (run.getTemplate() != null && run.getTemplate().getTemplateId() != null) {
            itemLabelsById = templateItemRepository
                    .findByTemplateTemplateIdOrderBySortOrderAsc(run.getTemplate().getTemplateId())
                    .stream()
                    .collect(Collectors.toMap(
                            ChecklistTemplateItem::getItemId,
                            ChecklistTemplateItem::getLabel,
                            (existing, replacement) -> existing));
        } else {
            itemLabelsById = Collections.emptyMap();
        }

        return ChecklistRunResponse.builder()
                .runId(run.getRunId())
                .templateId(run.getTemplate() != null ? run.getTemplate().getTemplateId() : null)
                .templateTitle(run.getTemplate() != null ? run.getTemplate().getTitle() : null)
                .templateDescription(run.getTemplate() != null ? run.getTemplate().getDescription() : null)
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
                        .map(item -> mapToItemResponse(item, itemLabelsById))
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private ChecklistRunItemResponse mapToItemResponse(ChecklistRunItem item) {
        return mapToItemResponse(item, Collections.emptyMap());
    }

    private ChecklistRunItemResponse mapToItemResponse(ChecklistRunItem item, Map<Long, String> itemLabelsById) {
        return ChecklistRunItemResponse.builder()
                .runItemId(item.getRunItemId())
                .runId(item.getRun() != null ? item.getRun().getRunId() : null)
                .templateItemId(item.getTemplateItemId())
                .templateItemLabel(itemLabelsById.get(item.getTemplateItemId()))
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

    private Long resolveUserId(CustomUserDetails userDetails) {
        return userDetails != null ? userDetails.getUserId() : 0L;
    }

    private void requireAnyRole(String... roles) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AccessDeniedException("Missing authentication");
        }

        for (String role : roles) {
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.equals(authority.getAuthority()));
            if (hasRole) {
                return;
            }
        }

        throw new AccessDeniedException("Insufficient permissions");
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
