package com.example.InternalControl.controller.document;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.document.OrganizationDocumentVersion;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.storage.BlobStorageService;
import com.example.InternalControl.service.document.DocumentService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/files", "/api/files"})
@Tag(name = "File Management", description = "Endpoints for uploading and downloading organization documents")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

  private final DocumentService documentService;
  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepo;
  private final OrganizationDocumentVersionRepository versionRepo;
  private final ObjectProvider<UserOrganizationService> userOrgServiceProvider;

  public FileController(DocumentService documentService,
                        BlobStorageService blobStorageService,
                        OrganizationDocumentRepository documentRepo,
                        OrganizationDocumentVersionRepository versionRepo,
                        ObjectProvider<UserOrganizationService> userOrgServiceProvider) {
    this.documentService = documentService;
    this.blobStorageService = blobStorageService;
    this.documentRepo = documentRepo;
    this.versionRepo = versionRepo;
    this.userOrgServiceProvider = userOrgServiceProvider;
  }

  @Operation(
      summary = "List all documents",
      description = "Returns a list of all documents for the specified organization."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden - not a member of organization")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<OrganizationDocument>> listDocuments(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestParam(required = false) Integer orgNumber,
      @RequestHeader(value = "X-Org-Number", required = false) Integer orgNumberHeader,
      @AuthenticationPrincipal CustomUserDetails userDetails,

      @Parameter(description = "Optional document type filter")
      @RequestParam(required = false) String category) {

    requireAnyRole("ROLE_EMPLOYEE", "ROLE_MANAGER", "ROLE_ADMIN");
    Integer resolvedOrgNumber = resolveOrgNumber(orgNumber, orgNumberHeader);
    validateUserOrganizationAccess(resolveUserId(userDetails), resolvedOrgNumber);

    List<OrganizationDocument> documents;
    if (category != null && !category.isEmpty()) {
      documents = documentRepo.findByOrgNumberAndDocumentType(resolvedOrgNumber, category);
    } else {
      documents = documentRepo.findByOrgNumberAndActiveTrue(resolvedOrgNumber);
    }
    return ResponseEntity.ok(documents);
  }

  @Operation(
      summary = "Upload a document",
      description = "Uploads a file to Azure Blob Storage and creates metadata records."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid file name, empty file, or invalid input"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden - not a member of organization"),
      @ApiResponse(responseCode = "500", description = "Storage or database error")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<?> upload(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestParam(required = false) Integer orgNumber,
      @RequestHeader(value = "X-Org-Number", required = false) Integer orgNumberHeader,
      @AuthenticationPrincipal CustomUserDetails userDetails,

      @Parameter(description = "The file to upload", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = "Document type")
      @RequestParam(defaultValue = "other") String documentType,

      @Parameter(description = "Storage directory within the tenant's blob container")
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    requireAnyRole("ROLE_EMPLOYEE", "ROLE_MANAGER", "ROLE_ADMIN");
    Integer resolvedOrgNumber = resolveOrgNumber(orgNumber, orgNumberHeader);
    validateUserOrganizationAccess(resolveUserId(userDetails), resolvedOrgNumber);

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
    }

    OrganizationDocument doc = documentService.uploadDocument(resolvedOrgNumber, file, documentType, directory);
    return ResponseEntity.status(HttpStatus.CREATED).body(doc);
  }

  @Operation(
      summary = "Download a document",
      description = "Downloads the current version of a document by its ID."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden - not a member of organization"),
      @ApiResponse(responseCode = "404", description = "Document not found")
  })
  @GetMapping("/download/{documentId}")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<byte[]> download(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestParam(required = false) Integer orgNumber,
      @RequestHeader(value = "X-Org-Number", required = false) Integer orgNumberHeader,
      @AuthenticationPrincipal CustomUserDetails userDetails,

      @Parameter(description = "ID of the document to download", required = true)
      @PathVariable Long documentId) {

    requireAnyRole("ROLE_EMPLOYEE", "ROLE_MANAGER", "ROLE_ADMIN");
    Integer resolvedOrgNumber = resolveOrgNumber(orgNumber, orgNumberHeader);
    validateUserOrganizationAccess(resolveUserId(userDetails), resolvedOrgNumber);

    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, resolvedOrgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    OrganizationDocumentVersion version = versionRepo
        .findByDocumentIdAndVersionNumber(documentId, doc.getCurrentVersion())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    ByteArrayOutputStream stream = blobStorageService.downloadFile(resolvedOrgNumber, version.getAzureBlobName());

    String safeFilename = version.getOriginalFilename()
        .replaceAll("[^a-zA-Z0-9.\\-_]", "_")
        .replaceAll("\\.{2,}", "_");

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(version.getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + safeFilename + "\"")
        .body(stream.toByteArray());
  }

  private Long resolveUserId(CustomUserDetails userDetails) {
    return userDetails != null ? userDetails.getUserId() : 0L;
  }

  private Integer resolveOrgNumber(Integer orgNumber, Integer orgNumberHeader) {
    Integer resolved = orgNumber != null ? orgNumber : orgNumberHeader;
    if (resolved == null) {
      throw new IllegalArgumentException("Organization number is required");
    }
    return resolved;
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
    UserOrganizationService userOrgService = userOrgServiceProvider.getIfAvailable();
    if (userOrgService == null) {
      return;
    }
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }
}
