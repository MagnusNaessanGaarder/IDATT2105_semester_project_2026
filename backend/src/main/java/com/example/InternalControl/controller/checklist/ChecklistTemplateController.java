package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistTemplateItemCreateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateResponse;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.checklist.ChecklistTemplateService;
import com.example.InternalControl.service.settings.OrganizationModuleAccessService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.stream.Collectors;

/**
 * REST Controller for Checklist Template operations.
 */
@RestController
@RequestMapping({"/api/v1/checklists/templates", "/api/checklists/templates"})
@Tag(name = "Checklist Templates", description = "Manage checklist templates")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    private final ChecklistTemplateService templateService;
    private final UserOrganizationService userOrgService;
    private final OrganizationModuleAccessService moduleAccessService;

    @Operation(summary = "Get all templates for organization")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<List<ChecklistTemplateResponse>> getTemplates(
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getTemplatesByOrg(orgNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<ChecklistTemplateResponse> getTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(mapToResponse(templateService.getTemplate(id, orgNumber)));
    }

    @Operation(summary = "Get templates by module type")
    @GetMapping("/module/{moduleType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<List<ChecklistTemplateResponse>> getTemplatesByModule(
            @PathVariable ModuleType moduleType,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);
        moduleAccessService.ensureModuleEnabled(orgNumber, moduleType);
        return ResponseEntity.ok(templateService.getTemplatesByModule(orgNumber, moduleType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get active templates")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'COOK', 'BARTENDER', 'WAITER')")
    public ResponseEntity<List<ChecklistTemplateResponse>> getActiveTemplates(
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_COOK", "ROLE_BARTENDER", "ROLE_WAITER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(templateService.getActiveTemplates(orgNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Create new template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplateResponse> createTemplate(
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ModuleType moduleType = requestDto.getModuleType() != null ? requestDto.getModuleType() : ModuleType.FOOD;
        moduleAccessService.ensureModuleEnabled(orgNumber, moduleType);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .moduleType(moduleType)
                .frequency(requestDto.getFrequency() != null ? requestDto.getFrequency() : Frequency.DAILY)
                .build();
        applyTemplateItems(template, requestDto.getItems());

        ChecklistTemplate created = templateService.createTemplate(template, orgNumber, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getTemplateId())
                .toUri();

        return ResponseEntity.created(location).body(mapToResponse(created));
    }

    @Operation(summary = "Update template")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChecklistTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ChecklistTemplateCreateRequest requestDto,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);

        ModuleType moduleType = requestDto.getModuleType() != null ? requestDto.getModuleType() : ModuleType.FOOD;
        moduleAccessService.ensureModuleEnabled(orgNumber, moduleType);

        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .moduleType(moduleType)
                .frequency(requestDto.getFrequency() != null ? requestDto.getFrequency() : Frequency.DAILY)
                .build();
        applyTemplateItems(template, requestDto.getItems());

        return ResponseEntity.ok(mapToResponse(templateService.updateTemplate(id, template, orgNumber)));
    }

    @Operation(summary = "Delete template (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        Long userId = resolveUserId(userDetails);
        validateUserOrganizationAccess(userId, orgNumber);
        templateService.deleteTemplate(id, orgNumber);
        return ResponseEntity.noContent().build();
    }

    private ChecklistTemplateResponse mapToResponse(ChecklistTemplate template) {
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
                .items(template.getItems() != null ? template.getItems().stream()
                        .map(this::mapItemToResponse)
                        .collect(Collectors.toList()) : List.of())
                .build();
    }

    private ChecklistTemplateItemResponse mapItemToResponse(ChecklistTemplateItem item) {
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

    private void applyTemplateItems(ChecklistTemplate template, List<ChecklistTemplateItemCreateRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return;
        }

        for (ChecklistTemplateItemCreateRequest itemRequest : itemRequests) {
            ChecklistTemplateItem item = ChecklistTemplateItem.builder()
                    .sortOrder(itemRequest.getSortOrder())
                    .label(itemRequest.getLabel())
                    .description(itemRequest.getDescription())
                    .itemType(itemRequest.getItemType())
                    .isRequired(itemRequest.getIsRequired() != null ? itemRequest.getIsRequired() : Boolean.TRUE)
                    .expectedText(itemRequest.getExpectedText())
                    .expectedNumericMin(itemRequest.getExpectedNumericMin())
                    .expectedNumericMax(itemRequest.getExpectedNumericMax())
                    .choiceOptionsJson(itemRequest.getChoiceOptionsJson())
                    .build();
            template.addItem(item);
        }
    }
}
