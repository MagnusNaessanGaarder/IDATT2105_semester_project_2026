package com.example.InternalControl.controller;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.model.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.service.ChecklistTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final ChecklistTemplateService templateService;

    @Operation(summary = "Get all templates for organization")
    @GetMapping
    public ResponseEntity<List<ChecklistTemplate>> getTemplates(
            @RequestParam Integer orgNumber) {
        return ResponseEntity.ok(templateService.getTemplatesByOrg(orgNumber));
    }

    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ChecklistTemplate> getTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber) {
        return ResponseEntity.ok(templateService.getTemplate(id, orgNumber));
    }

    @Operation(summary = "Get templates by module type")
    @GetMapping("/module/{moduleType}")
    public ResponseEntity<List<ChecklistTemplate>> getTemplatesByModule(
            @PathVariable ModuleType moduleType,
            @RequestParam Integer orgNumber) {
        return ResponseEntity.ok(templateService.getTemplatesByModule(orgNumber, moduleType));
    }

    @Operation(summary = "Get active templates")
    @GetMapping("/active")
    public ResponseEntity<List<ChecklistTemplate>> getActiveTemplates(
            @RequestParam Integer orgNumber) {
        return ResponseEntity.ok(templateService.getActiveTemplates(orgNumber));
    }

    @Operation(summary = "Create new template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplate> createTemplate(
            @Valid @RequestBody ChecklistTemplateCreateRequest request,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal UserDetails user) {

        Long userId = extractUserId(user);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .moduleType(request.getModuleType())
                .frequency(request.getFrequency())
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
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplate> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ChecklistTemplateCreateRequest request,
            @RequestParam Integer orgNumber) {

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .moduleType(request.getModuleType())
                .frequency(request.getFrequency())
                .build();

        return ResponseEntity.ok(templateService.updateTemplate(id, template, orgNumber));
    }

    @Operation(summary = "Delete template (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber) {
        templateService.deleteTemplate(id, orgNumber);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(UserDetails user) {
        // TODO: Implement proper user ID extraction from JWT token
        return 1L;
    }
}
