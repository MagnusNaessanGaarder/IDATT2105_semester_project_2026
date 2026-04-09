package com.example.InternalControl.controller.document;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.document.OrganizationDocumentVersion;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import com.example.InternalControl.service.storage.BlobStorageService;
import com.example.InternalControl.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "Endpoints for uploading and downloading organization documents")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

  private final DocumentService documentService;
  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepo;
  private final OrganizationDocumentVersionRepository versionRepo;

  public FileController(DocumentService documentService,
                        BlobStorageService blobStorageService,
                        OrganizationDocumentRepository documentRepo,
                        OrganizationDocumentVersionRepository versionRepo) {
    this.documentService = documentService;
    this.blobStorageService = blobStorageService;
    this.documentRepo = documentRepo;
    this.versionRepo = versionRepo;
  }

  @Operation(
      summary = "List all documents",
      description = "Returns a list of all documents for the specified organization."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  @GetMapping
  public ResponseEntity<List<OrganizationDocument>> listDocuments(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestHeader("X-Org-Number") Integer orgNumber,

      @Parameter(description = "Optional document type filter")
      @RequestParam(required = false) String category) {

    List<OrganizationDocument> documents;
    if (category != null && !category.isEmpty()) {
      documents = documentRepo.findByOrgNumberAndDocumentType(orgNumber, category);
    } else {
      documents = documentRepo.findByOrgNumberAndActiveTrue(orgNumber);
    }
    return ResponseEntity.ok(documents);
  }

  @Operation(
      summary = "Upload a document",
      description = "Uploads a file to Azure Blob Storage and creates metadata records."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid file name, empty file, or invalid input"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "500", description = "Storage or database error")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestHeader("X-Org-Number") Integer orgNumber,

      @Parameter(description = "The file to upload", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = "Document type")
      @RequestParam(defaultValue = "other") String documentType,

      @Parameter(description = "Storage directory within the tenant's blob container")
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
    }

    OrganizationDocument doc = documentService.uploadDocument(orgNumber, file, documentType, directory);
    return ResponseEntity.ok(doc);
  }

  @Operation(
      summary = "Download a document",
      description = "Downloads the current version of a document by its ID."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "404", description = "Document not found")
  })
  @GetMapping("/download/{documentId}")
  public ResponseEntity<byte[]> download(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestHeader("X-Org-Number") Integer orgNumber,

      @Parameter(description = "ID of the document to download", required = true)
      @PathVariable Long documentId) {

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
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + safeFilename + "\"")
        .body(stream.toByteArray());
  }

  @PutMapping("/{documentId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  @Operation(summary = "Update document metadata")
  public ResponseEntity<OrganizationDocument> updateDocumentMetadata(
      @RequestHeader("X-Org-Number") Integer orgNumber,
      @PathVariable Long documentId,
      @RequestBody Map<String, String> payload) {

    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    String title = payload.get("title");
    String description = payload.get("description");

    if (title != null && !title.isBlank()) {
      doc.setTitle(title.trim());
    }
    if (description != null) {
      doc.setDescription(description.trim().isEmpty() ? null : description.trim());
    }

    OrganizationDocument saved = documentRepo.save(doc);
    return ResponseEntity.ok(saved);
  }

  @DeleteMapping("/{documentId}")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  @Operation(summary = "Delete document (soft delete)")
  public ResponseEntity<Void> deleteDocument(
      @RequestHeader("X-Org-Number") Integer orgNumber,
      @PathVariable Long documentId) {

    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    doc.setActive(false);
    documentRepo.save(doc);

    return ResponseEntity.noContent().build();
  }
}
