package com.example.InternalControl.controller.deviation;

import com.example.InternalControl.dto.deviation.DeviationReportDto;
import com.example.InternalControl.dto.deviation.request.DeviationActionRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportUpdateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationStatusUpdateRequest;
import com.example.InternalControl.service.deviation.mapper.DeviationReportMapper;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.shared.enums.DeviationStatus;
import com.example.InternalControl.shared.enums.Severity;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.deviation.DeviationReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Deviation/Incident Report operations.
 * Uses DTOs to avoid exposing entity relationships.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/deviations")
@Tag(name = "Deviation Reports", description = "Manage deviation and incident reports")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeviationReportController {

    private final DeviationReportService deviationReportService;
    private final DeviationReportMapper deviationReportMapper;
    private final AuthenticationFacade authenticationFacade;

    @Operation(summary = "Get all deviation reports for organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation reports"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<List<DeviationReportDto>> getReports(
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<DeviationReport> reports = deviationReportService.getReportsByOrg(orgNumber);
        return ResponseEntity.ok(reports.stream()
            .map(deviationReportMapper::toDto)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Get deviation report by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation report"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeviationReportDto> getReport(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport report = deviationReportService.getReport(id, orgNumber);
        return ResponseEntity.ok(deviationReportMapper.toDto(report));
    }

    @Operation(summary = "Search deviation reports with filters")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered deviation reports"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/search")
    public ResponseEntity<List<DeviationReportDto>> searchReports(
            @RequestParam Integer orgNumber,
            @RequestParam(required = false) DeviationStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<DeviationReport> reports = deviationReportService.searchReports(
                orgNumber, status, severity, assignedToId, fromDate, toDate);
        return ResponseEntity.ok(reports.stream()
            .map(deviationReportMapper::toDto)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Get deviation reports by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation reports by status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DeviationReportDto>> getReportsByStatus(
            @PathVariable DeviationStatus status,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<DeviationReport> reports = deviationReportService.getReportsByStatus(orgNumber, status);
        return ResponseEntity.ok(reports.stream()
            .map(deviationReportMapper::toDto)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Get deviation reports by severity")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation reports by severity"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<DeviationReportDto>> getReportsBySeverity(
            @PathVariable Severity severity,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<DeviationReport> reports = deviationReportService.getReportsBySeverity(orgNumber, severity);
        return ResponseEntity.ok(reports.stream()
            .map(deviationReportMapper::toDto)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Get deviation reports assigned to a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved assigned deviation reports"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/assigned/{assignedToId}")
    public ResponseEntity<List<DeviationReportDto>> getReportsAssignedTo(
            @PathVariable Long assignedToId,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<DeviationReport> reports = deviationReportService.getReportsAssignedTo(assignedToId, orgNumber);
        return ResponseEntity.ok(reports.stream()
            .map(deviationReportMapper::toDto)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Create new deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Deviation report created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Organization or referenced user not found")
    })
    @PostMapping
    public ResponseEntity<DeviationReportDto> createReport(
            @Valid @RequestBody DeviationReportCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest httpRequest) {
        Long userId = authenticationFacade.extractAndValidateUser(httpRequest, orgNumber);

        DeviationReport created = deviationReportService.createReport(requestDto, orgNumber, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getReportId())
                .toUri();

        return ResponseEntity.created(location).body(deviationReportMapper.toDto(created));
    }

    @Operation(summary = "Update deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deviation report updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DeviationReportDto> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody DeviationReportUpdateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport updated = deviationReportService.updateReport(id, requestDto, orgNumber);
        return ResponseEntity.ok(deviationReportMapper.toDto(updated));
    }

    @Operation(summary = "Delete deviation report (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deviation report deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        deviationReportService.deleteReport(id, orgNumber);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update deviation report status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DeviationReportDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DeviationStatusUpdateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport updated = deviationReportService.updateStatus(
                id, requestDto.getStatus(), orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(updated));
    }

    @Operation(summary = "Assign deviation report to a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deviation report assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report or user not found")
    })
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DeviationReportDto> assignReport(
            @PathVariable Long id,
            @RequestParam Long assignedToUserId,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport assigned = deviationReportService.assignReport(
                id, assignedToUserId, orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(assigned));
    }

    @Operation(summary = "Add immediate action to deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Immediate action added successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PostMapping("/{id}/immediate-action")
    public ResponseEntity<DeviationReportDto> addImmediateAction(
            @PathVariable Long id,
            @Valid @RequestBody DeviationActionRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport updated = deviationReportService.addImmediateAction(
                id, requestDto, orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(updated));
    }

    @Operation(summary = "Add cause analysis to deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cause analysis added successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PostMapping("/{id}/cause-analysis")
    public ResponseEntity<DeviationReportDto> addCauseAnalysis(
            @PathVariable Long id,
            @Valid @RequestBody DeviationActionRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport updated = deviationReportService.addCauseAnalysis(
                id, requestDto, orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(updated));
    }

    @Operation(summary = "Add corrective action to deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Corrective action added successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PostMapping("/{id}/corrective-action")
    public ResponseEntity<DeviationReportDto> addCorrectiveAction(
            @PathVariable Long id,
            @Valid @RequestBody DeviationActionRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport updated = deviationReportService.addCorrectiveAction(
                id, requestDto, orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(updated));
    }

    @Operation(summary = "Complete deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Completion added successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<DeviationReportDto> completeReport(
            @PathVariable Long id,
            @Valid @RequestBody DeviationActionRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport updated = deviationReportService.completeReport(
                id, requestDto, orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(updated));
    }

    @Operation(summary = "Close deviation report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deviation report closed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Deviation report not found")
    })
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DeviationReportDto> closeReport(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        DeviationReport closed = deviationReportService.closeReport(id, orgNumber, userId);
        return ResponseEntity.ok(deviationReportMapper.toDto(closed));
    }

    @Operation(summary = "Get count of open deviation reports")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/count/open")
    public ResponseEntity<Long> getOpenReportCount(
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        return ResponseEntity.ok(deviationReportService.getOpenReportCount(orgNumber));
    }
}
