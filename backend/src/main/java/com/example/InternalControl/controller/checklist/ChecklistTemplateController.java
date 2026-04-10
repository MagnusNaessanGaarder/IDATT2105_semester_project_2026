package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistTemplateItemCreateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateResponse;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.checklist.ChecklistTemplateService;
import com.example.InternalControl.service.settings.OrganizationModuleAccessService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
  private final OrganizationModuleAccessService moduleAccessService;

  @Operation(summary = "Get all templates for organization")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved templates"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<ChecklistTemplateResponse>> getTemplates(
      @Parameter(description = "Organization number", required = true) @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    return ResponseEntity.ok(templateService.getTemplatesByOrg(orgNumber).stream()
        .map(this::toResponse).collect(Collectors.toList()));
  }

  @Operation(summary = "Get template by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved template"),
      @ApiResponse(responseCode = "404", description = "Template not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<ChecklistTemplateResponse> getTemplate(
      @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    return ResponseEntity.ok(toResponse(templateService.getTemplate(id, orgNumber)));
  }

  @Operation(summary = "Get templates by module type")
  @GetMapping("/module/{moduleType}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<ChecklistTemplateResponse>> getTemplatesByModule(
      @PathVariable ModuleType moduleType,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    moduleAccessService.ensureModuleEnabled(orgNumber, moduleType);
    return ResponseEntity.ok(templateService.getTemplatesByModule(orgNumber, moduleType).stream()
        .map(this::toResponse).collect(Collectors.toList()));
  }

  @Operation(summary = "Get active templates")
  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<ChecklistTemplateResponse>> getActiveTemplates(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    return ResponseEntity.ok(templateService.getActiveTemplates(orgNumber).stream()
        .map(this::toResponse).collect(Collectors.toList()));
  }

  @Operation(summary = "Create new template")
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<ChecklistTemplateResponse> createTemplate(
      @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    moduleAccessService.ensureModuleEnabled(orgNumber, requestDto.getModuleType());

    ChecklistTemplate template = ChecklistTemplate.builder()
        .title(requestDto.getTitle())
        .description(requestDto.getDescription())
        .moduleType(requestDto.getModuleType())
        .frequency(requestDto.getFrequency())
        .items(buildItems(requestDto.getItems()))
        .build();

    ChecklistTemplate created = templateService.createTemplate(template, orgNumber, userDetails.getUserId());

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}").buildAndExpand(created.getTemplateId()).toUri();

    return ResponseEntity.created(location).body(toResponse(created));
  }

  @Operation(summary = "Update template")
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<ChecklistTemplateResponse> updateTemplate(
      @PathVariable Long id,
      @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    moduleAccessService.ensureModuleEnabled(orgNumber, requestDto.getModuleType());

    ChecklistTemplate template = ChecklistTemplate.builder()
        .title(requestDto.getTitle())
        .description(requestDto.getDescription())
        .moduleType(requestDto.getModuleType())
        .frequency(requestDto.getFrequency())
        .items(buildItems(requestDto.getItems()))
        .build();

    return ResponseEntity.ok(toResponse(templateService.updateTemplate(id, template, orgNumber)));
  }

  @Operation(summary = "Delete template (soft delete)")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Void> deleteTemplate(
      @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    templateService.deleteTemplate(id, orgNumber);
    return ResponseEntity.noContent().build();
  }

  private List<ChecklistTemplateItem> buildItems(List<ChecklistTemplateItemCreateRequest> itemRequests) {
    if (itemRequests == null) {
      return new ArrayList<>();
    }
    return itemRequests.stream().map(req -> ChecklistTemplateItem.builder()
        .sortOrder(req.getSortOrder())
        .label(req.getLabel())
        .description(req.getDescription())
        .itemType(req.getItemType())
        .isRequired(req.getIsRequired() != null ? req.getIsRequired() : true)
        .expectedText(req.getExpectedText())
        .expectedNumericMin(req.getExpectedNumericMin())
        .expectedNumericMax(req.getExpectedNumericMax())
        .choiceOptionsJson(req.getChoiceOptionsJson())
        .build()
    ).collect(Collectors.toList());
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }

  private ChecklistTemplateItemResponse toItemResponse(ChecklistTemplateItem item) {
    return ChecklistTemplateItemResponse.builder()
        .itemId(item.getItemId())
        .templateId(item.getTemplate() != null ? item.getTemplate().getTemplateId() : null)
        .sortOrder(item.getSortOrder())
        .label(item.getLabel())
        .description(item.getDescription())
        .itemType(item.getItemType())
        .isRequired(item.getIsRequired())
        .expectedText(item.getExpectedText())
        .expectedNumericMin(item.getExpectedNumericMin())
        .expectedNumericMax(item.getExpectedNumericMax())
        .choiceOptionsJson(item.getChoiceOptionsJson())
        .build();
  }

  private ChecklistTemplateResponse toResponse(ChecklistTemplate template) {
    List<ChecklistTemplateItemResponse> items = template.getItems() == null ? List.of()
        : template.getItems().stream().map(this::toItemResponse).collect(Collectors.toList());

    return ChecklistTemplateResponse.builder()
        .templateId(template.getTemplateId())
        .orgNumber(template.getOrgNumber())
        .moduleType(template.getModuleType())
        .title(template.getTitle())
        .description(template.getDescription())
        .frequency(template.getFrequency())
        .isActive(template.getIsActive())
        .createdByUserId(template.getCreatedByUserId())
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .items(items)
        .build();
  }
}
