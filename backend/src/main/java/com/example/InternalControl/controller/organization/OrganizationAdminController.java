package com.example.InternalControl.controller.organization;

import com.example.InternalControl.model.organization.Organization;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for platform-level organisation management.
 * Restricted to users with ROLE_SYSADMIN — no org membership required.
 */
@RestController
@RequestMapping("/api/v1/sysadmin/organizations")
@PreAuthorize("hasRole('SYSADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sysadmin – Organizations",
        description = "Platform-level organization management. Requires SYSADMIN role.")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationAdminController {

    private final OrganizationRepository organizationRepository;

    @GetMapping
    @Operation(
            summary = "List all organizations",
            description = "Returns every organization on the platform regardless of active status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – SYSADMIN role required")
    })
    public ResponseEntity<List<Organization>> listAll() {
        log.info("Sysadmin listing all organizations");
        return ResponseEntity.ok(organizationRepository.findAll());
    }

    @PostMapping
    @Operation(
            summary = "Register a new organization",
            description = "Creates a new organization. The orgNumber must be unique and a valid 9-digit Norwegian organization number."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organization created successfully"),
            @ApiResponse(responseCode = "409", description = "Organization number already exists"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – SYSADMIN role required")
    })
    public ResponseEntity<Organization> create(@RequestBody OrgRequest request) {
        log.info("Sysadmin creating organization: {}", request.orgNumber());

        if (organizationRepository.existsById(request.orgNumber())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Organization org = Organization.builder()
                .orgNumber(request.orgNumber())
                .legalName(request.legalName())
                .displayName(request.displayName() != null ? request.displayName() : request.legalName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .isActive(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(organizationRepository.save(org));
    }

    @PutMapping("/{orgNumber}")
    @Operation(
            summary = "Update an organization",
            description = "Updates mutable fields on an existing organization. Only non-null fields in the request body are applied."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization updated successfully"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – SYSADMIN role required")
    })
    public ResponseEntity<Organization> update(
            @Parameter(description = "Norwegian organization number (9 digits)", example = "987654321")
            @PathVariable Integer orgNumber,
            @RequestBody OrgRequest request) {

        log.info("Sysadmin updating organization: {}", orgNumber);

        Organization org = organizationRepository.findById(orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgNumber));

        if (request.legalName() != null)    org.setLegalName(request.legalName());
        if (request.displayName() != null)  org.setDisplayName(request.displayName());
        if (request.contactEmail() != null) org.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) org.setContactPhone(request.contactPhone());
        if (request.isActive() != null)     org.setIsActive(request.isActive());

        return ResponseEntity.ok(organizationRepository.save(org));
    }

    @DeleteMapping("/{orgNumber}")
    @Operation(
            summary = "Deactivate an organization",
            description = "Soft-deletes an organization by setting isActive = false. Users belonging to this org lose access. Can be reversed via the update endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Organization deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – SYSADMIN role required")
    })
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Norwegian organization number (9 digits)", example = "987654321")
            @PathVariable Integer orgNumber) {

        log.info("Sysadmin deactivating organization: {}", orgNumber);

        Organization org = organizationRepository.findById(orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgNumber));

        org.setIsActive(false);
        organizationRepository.save(org);
        return ResponseEntity.noContent().build();
    }

    /**
     * Request body for create and update operations.
     * All fields except orgNumber and legalName are optional on update.
     */
    public record OrgRequest(
            Integer orgNumber,
            String legalName,
            String displayName,
            String contactEmail,
            String contactPhone,
            Boolean isActive
    ) {}
}