package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.checklist.ChecklistTemplateService;
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
import java.util.List;

/**
 * REST Controller for Checklist Template operations.
 */
@RestController
@RequestMapping("/api/v1/checklists/templates")
@Tag(name = "Checklist Templates", description = "Manage checklist templates")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    private final ChecklistTemplateService templateService;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get all templates for organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved templates"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<ChecklistTemplate>> getTemplates(
                @Parameter(description = "Organization number identifying the tenant", required = true)
                @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplatesByOrg(orgNumber));
    }

    @Operation(summary = "Get template by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved template"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ChecklistTemplate> getTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplate(id, orgNumber));
    }

    @Operation(summary = "Get templates by module type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved templates"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/module/{moduleType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<ChecklistTemplate>> getTemplatesByModule(
                @Parameter(description = "Module type (IK_MAT or IK_ALKOHOL)")
                @PathVariable ModuleType moduleType,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplatesByModule(orgNumber, moduleType));
    }

    @Operation(summary = "Get active templates")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved templates"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<ChecklistTemplate>> getActiveTemplates(
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getActiveTemplates(orgNumber));
    }

    @Operation(summary = "Create new template")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplate> createTemplate(
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .moduleType(requestDto.getModuleType())
                .frequency(requestDto.getFrequency())
                .build();

        ChecklistTemplate created = templateService.createTemplate(template, orgNumber, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getTemplateId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Update template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplate> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .moduleType(requestDto.getModuleType())
                .frequency(requestDto.getFrequency())
                .build();

        return ResponseEntity.ok(templateService.updateTemplate(id, template, orgNumber));
    }

    @Operation(summary = "Delete template (soft delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        templateService.deleteTemplate(id, orgNumber);
        return ResponseEntity.noContent().build();
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
