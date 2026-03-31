package com.example.InternalControl.controller;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.model.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.ChecklistTemplateService;
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
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/checklists/templates")
@Tag(name = "Checklist Templates", description = "Manage checklist templates")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    /**
     * Length of the "Bearer " prefix in Authorization header.
     */
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final ChecklistTemplateService templateService;

    private final JwtService jwtService;

    private final UserOrganizationService userOrgService;

    /**
     * Get all templates for organization.
     *
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return list of templates
     */
    @Operation(summary = "Get all templates for organization")
    @GetMapping
    public ResponseEntity<List<ChecklistTemplate>> getTemplates(
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplatesByOrg(orgNumber));
    }

    /**
     * Get template by ID.
     *
     * @param id the template ID
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return the template
     */
    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ChecklistTemplate> getTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplate(id, orgNumber));
    }

    /**
     * Get templates by module type.
     *
     * @param moduleType the module type
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return list of templates
     */
    @Operation(summary = "Get templates by module type")
    @GetMapping("/module/{moduleType}")
    public ResponseEntity<List<ChecklistTemplate>> getTemplatesByModule(
            @PathVariable ModuleType moduleType,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplatesByModule(orgNumber, moduleType));
    }

    /**
     * Get active templates.
     *
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return list of active templates
     */
    @Operation(summary = "Get active templates")
    @GetMapping("/active")
    public ResponseEntity<List<ChecklistTemplate>> getActiveTemplates(
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getActiveTemplates(orgNumber));
    }

    /**
     * Create new template.
     *
     * @param requestDto the create request
     * @param orgNumber the organization number
     * @param httpRequest the HTTP request
     * @return the created template
     */
    @Operation(summary = "Create new template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplate> createTemplate(
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest httpRequest) {

        Long userId = extractUserId(httpRequest);
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

    /**
     * Update template.
     *
     * @param id the template ID
     * @param requestDto the update request
     * @param orgNumber the organization number
     * @param httpRequest the HTTP request
     * @return the updated template
     */
    @Operation(summary = "Update template")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplate> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest httpRequest) {

        Long userId = extractUserId(httpRequest);
        validateUserOrganizationAccess(userId, orgNumber);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .moduleType(requestDto.getModuleType())
                .frequency(requestDto.getFrequency())
                .build();

        return ResponseEntity.ok(templateService.updateTemplate(id, template, orgNumber));
    }

    /**
     * Delete template (soft delete).
     *
     * @param id the template ID
     * @param orgNumber the organization number
     * @param request the HTTP request
     * @return no content
     */
    @Operation(summary = "Delete template (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        validateUserOrganizationAccess(userId, orgNumber);
        templateService.deleteTemplate(id, orgNumber);
        return ResponseEntity.noContent().build();
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
