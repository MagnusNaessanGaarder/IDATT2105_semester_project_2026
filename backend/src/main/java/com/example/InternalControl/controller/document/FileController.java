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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Management", description = "Endpoints for uploading and downloading organization documents")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
  private static final int  MAX_TITLE_LEN = 255;
  private static final int  MAX_DESC_LEN  = 1000;

  private final DocumentService documentService;
  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepo;
  private final OrganizationDocumentVersionRepository versionRepo;
  private final UserOrganizationService userOrgService;

  public FileController(DocumentService documentService,
                        BlobStorageService blobStorageService,
                        OrganizationDocumentRepository documentRepo,
                        OrganizationDocumentVersionRepository versionRepo,
                        UserOrganizationService userOrgService) {
    this.documentService = documentService;
    this.blobStorageService = blobStorageService;
    this.documentRepo = documentRepo;
    this.versionRepo = versionRepo;
    this.userOrgService = userOrgService;
  }

  @Operation(summary = "List all documents",
      description = "Returns all active documents for the specified organization.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<OrganizationDocument>> listDocuments(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) String category) {

    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);

    List<OrganizationDocument> documents = (category != null && !category.isEmpty())
        ? documentRepo.findByOrgNumberAndDocumentType(orgNumber, category)
        : documentRepo.findByOrgNumberAndActiveTrue(orgNumber);

    return ResponseEntity.ok(documents);
  }

  // ── Upload (new document) ─────────────────────────────────────────────────

  @Operation(summary = "Upload a document",
      description = "Uploads a file to Azure Blob Storage and creates a new document record.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "500", description = "Storage or database error")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<?> upload(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "The file to upload", required = true)
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "OTHER") String documentType,
      @Parameter(description = "Optional title — defaults to the filename (max 255 chars)")
      @RequestParam(required = false) String title,
      @Parameter(description = "Optional description (max 1000 chars)")
      @RequestParam(required = false) String description,
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    enforceFileSize(file);

    OrganizationDocument doc = documentService.uploadDocument(
        orgNumber, file, documentType,
        sanitiseText(title, MAX_TITLE_LEN),
        sanitiseText(description, MAX_DESC_LEN),
        directory);

    return ResponseEntity.status(HttpStatus.CREATED).body(doc);
  }

  @Operation(summary = "Upload a new version of an existing document",
      description = "Adds a new version to an existing document record, incrementing " +
                    "current_version. Title and description are updated only when provided.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "New version uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Document not found"),
      @ApiResponse(responseCode = "409", description = "Version already exists (concurrent upload)"),
      @ApiResponse(responseCode = "500", description = "Storage or database error")
  })
  @PostMapping(value = "/{documentId}/version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<OrganizationDocument> uploadNewVersion(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "ID of the document to add a version to", required = true)
      @PathVariable Long documentId,
      @Parameter(description = "The replacement file", required = true)
      @RequestParam("file") MultipartFile file,
      @Parameter(description = "Optional updated title (null/blank = keep existing)")
      @RequestParam(required = false) String title,
      @Parameter(description = "Optional updated description (null = keep existing)")
      @RequestParam(required = false) String description,
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);
    enforceFileSize(file);

    OrganizationDocument doc = documentService.uploadNewVersion(
        orgNumber, documentId, file,
        sanitiseText(title, MAX_TITLE_LEN),
        sanitiseText(description, MAX_DESC_LEN),
        directory);

    return ResponseEntity.ok(doc);
  }

  @Operation(summary = "Download a document",
      description = "Downloads the current version of a document by its ID.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Document not found")
  })
  @GetMapping("/download/{documentId}")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<byte[]> download(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long documentId) {

    validateUserOrganizationAccess(userDetails.getUserId(), orgNumber);

    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    OrganizationDocumentVersion version = versionRepo
        .findByDocumentIdAndVersionNumber(documentId, doc.getCurrentVersion())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    ByteArrayOutputStream stream = blobStorageService.downloadFile(orgNumber, version.getAzureBlobName());

    String safeFilename = version.getOriginalFilename()
        .replaceAll("[^a-zA-Z0-9.\\-_]", "_")
        .replaceAll("\\.{2,}", "_");

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(version.getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
        .body(stream.toByteArray());
  }


  private static void enforceFileSize(MultipartFile file) {
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
    }
  }

  /**
   * Strips control characters (except \t \n \r), collapses whitespace runs,
   * trims and caps to {@code maxLen}. Returns {@code null} for null/blank input.
   */
  private static String sanitiseText(String input, int maxLen) {
    if (input == null) return null;
    String cleaned = input.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");
    cleaned = cleaned.replaceAll("[ \t]{2,}", " ").strip();
    if (cleaned.isEmpty()) return null;
    return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen);
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }
}
