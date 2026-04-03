package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistRunItemUpdateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistRunItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistRunResponse;
import com.example.InternalControl.service.checklist.mapper.ChecklistRunMapper;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.shared.enums.RunStatus;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.checklist.ChecklistRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final ChecklistRunService runService;

    private final AuthenticationFacade authenticationFacade;

    private final ChecklistRunMapper runMapper;

    @Operation(summary = "Get all runs for organization")
    @GetMapping
    public ResponseEntity<List<ChecklistRunResponse>> getRuns(
            @RequestParam Integer orgNumber,
            @RequestParam(required = false) RunStatus status,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);

        List<ChecklistRun> runs;
        if (status != null) {
            runs = runService.getRunsByStatus(orgNumber, status);
        } else {
            runs = runService.getRunsByOrg(orgNumber);
        }

        return ResponseEntity.ok(runMapper.toResponseList(runs));
    }

    @Operation(summary = "Get run by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ChecklistRunResponse> getRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(runMapper.toResponse(run));
    }

    @Operation(summary = "Create new run from template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistRunResponse> createRun(
            @Valid @RequestBody ChecklistRunCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);

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

        return ResponseEntity.created(location).body(runMapper.toResponse(run));
    }

    @Operation(summary = "Complete a run")
    @PutMapping("/{id}/complete")
    public ResponseEntity<ChecklistRunResponse> completeRun(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);

        ChecklistRun run = runService.completeRun(id, orgNumber);
        return ResponseEntity.ok(runMapper.toResponse(run));
    }

    @Operation(summary = "Update run item (answer question)")
    @PutMapping("/{runId}/items/{itemId}")
    public ResponseEntity<ChecklistRunItemResponse> updateRunItem(
            @PathVariable Long runId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistRunItemUpdateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);

        ChecklistRunItem item = ChecklistRunItem.builder()
                .booleanValue(requestDto.getBooleanValue())
                .textValue(requestDto.getTextValue())
                .numericValue(requestDto.getNumericValue())
                .selectedChoice(requestDto.getSelectedChoice())
                .isDeviation(requestDto.getIsDeviation())
                .commentText(requestDto.getCommentText())
                .build();

        ChecklistRunItem updated = runService.updateRunItem(runId, itemId, item, orgNumber);
        return ResponseEntity.ok(runMapper.toItemResponse(updated));
    }

    @Operation(summary = "Get all items for a run")
    @GetMapping("/{id}/items")
    public ResponseEntity<List<ChecklistRunItemResponse>> getRunItems(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);

        ChecklistRun run = runService.getRun(id, orgNumber);
        return ResponseEntity.ok(runMapper.toResponse(run).getItems());
    }

}
