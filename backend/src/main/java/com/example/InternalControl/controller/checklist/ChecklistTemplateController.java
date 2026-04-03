package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateResponse;
import com.example.InternalControl.service.checklist.mapper.ChecklistTemplateMapper;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.shared.enums.ModuleType;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.checklist.ChecklistTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final ChecklistTemplateService templateService;

    private final AuthenticationFacade authenticationFacade;

    private final ChecklistTemplateMapper templateMapper;

    @Operation(summary = "Get all templates for organization")
    @GetMapping
    public ResponseEntity<List<ChecklistTemplateResponse>> getTemplates(
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<ChecklistTemplate> templates = templateService.getTemplatesByOrg(orgNumber);
        return ResponseEntity.ok(templateMapper.toResponseList(templates));
    }

    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ChecklistTemplateResponse> getTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        ChecklistTemplate template = templateService.getTemplate(id, orgNumber);
        return ResponseEntity.ok(templateMapper.toResponse(template));
    }

    @Operation(summary = "Get templates by module type")
    @GetMapping("/module/{moduleType}")
    public ResponseEntity<List<ChecklistTemplateResponse>> getTemplatesByModule(
            @PathVariable ModuleType moduleType,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<ChecklistTemplate> templates = templateService.getTemplatesByModule(orgNumber, moduleType);
        return ResponseEntity.ok(templateMapper.toResponseList(templates));
    }

    @Operation(summary = "Get active templates")
    @GetMapping("/active")
    public ResponseEntity<List<ChecklistTemplateResponse>> getActiveTemplates(
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        List<ChecklistTemplate> templates = templateService.getActiveTemplates(orgNumber);
        return ResponseEntity.ok(templateMapper.toResponseList(templates));
    }

    @Operation(summary = "Create new template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplateResponse> createTemplate(
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest httpRequest) {

        Long userId = authenticationFacade.extractAndValidateUser(httpRequest, orgNumber);

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

        return ResponseEntity.created(location).body(templateMapper.toResponse(created));
    }

    @Operation(summary = "Update template")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            HttpServletRequest httpRequest) {

        Long userId = authenticationFacade.extractAndValidateUser(httpRequest, orgNumber);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .moduleType(requestDto.getModuleType())
                .frequency(requestDto.getFrequency())
                .build();

        ChecklistTemplate updated = templateService.updateTemplate(id, template, orgNumber);
        return ResponseEntity.ok(templateMapper.toResponse(updated));
    }

    @Operation(summary = "Delete template (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            HttpServletRequest request) {
        Long userId = authenticationFacade.extractAndValidateUser(request, orgNumber);
        templateService.deleteTemplate(id, orgNumber);
        return ResponseEntity.noContent().build();
    }
}
