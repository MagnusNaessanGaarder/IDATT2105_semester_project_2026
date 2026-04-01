package com.example.InternalControl.controller;

import com.example.InternalControl.model.OrganizationDocument;
import com.example.InternalControl.model.OrganizationDocumentVersion;
import com.example.InternalControl.repository.OrganizationDocumentRepository;
import com.example.InternalControl.repository.OrganizationDocumentVersionRepository;
import com.example.InternalControl.service.BlobStorageService;
import com.example.InternalControl.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "Endpoints for uploading and downloading organization documents")
public class FileController {

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
      summary = "Upload a document",
      description = "Uploads a file to Azure Blob Storage and creates metadata records "
          + "in the organization_document and organization_document_version tables."
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

      @Parameter(description = "Document type (e.g. policy, procedure, certificate, attachment, other)")
      @RequestParam(defaultValue = "other") String documentType,

      @Parameter(description = "Storage directory within the tenant's blob container")
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    OrganizationDocument doc = documentService.uploadDocument(orgNumber, file, documentType, directory);
    return ResponseEntity.ok(doc);
  }

  @Operation(
      summary = "Download a document",
      description = "Downloads the current version of a document by its ID. "
          + "Only returns documents belonging to the specified organization."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "404", description = "Document not found or does not belong to the organization")
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
        .replaceAll("[^a-zA-Z0-9.\\-_ ]", "_");

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(version.getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + safeFilename + "\"")
        .body(stream.toByteArray());
  }
}