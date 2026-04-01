package com.example.InternalControl.controller;

import com.example.InternalControl.model.OrganizationDocument;
import com.example.InternalControl.model.OrganizationDocumentVersion;
import com.example.InternalControl.model.enums.DocumentType;
import com.example.InternalControl.repository.OrganizationDocumentRepository;
import com.example.InternalControl.repository.OrganizationDocumentVersionRepository;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.BlobStorageService;
import com.example.InternalControl.service.DocumentService;
import com.example.InternalControl.service.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "Endpoints for uploading and downloading organization documents")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class FileController {

  private static final int BEARER_PREFIX_LENGTH = 7;
  private static final int MAX_FILENAME_LENGTH = 255;
  private static final int MAX_BLOB_NAME_LENGTH = 512;

  private final DocumentService documentService;
  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepo;
  private final OrganizationDocumentVersionRepository versionRepo;
  private final JwtService jwtService;
  private final UserOrganizationService userOrgService;

  private Long extractUserId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
    }
    String token = authHeader.substring(BEARER_PREFIX_LENGTH);
    Long userId = jwtService.extractUserId(token);
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID not found in token");
    }
    return userId;
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }

  private void validateDocumentType(String documentType) {
    try {
      DocumentType.valueOf(documentType);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid document type. Must be one of: policy, procedure, training_material, " +
              "certificate, attachment, report_export, other");
    }
  }

  private void validateFilenameLength(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename cannot be empty");
    }
    if (filename.length() > MAX_FILENAME_LENGTH) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Filename exceeds maximum length of " + MAX_FILENAME_LENGTH + " characters");
    }
  }

  private void validateBlobNameLength(String blobName) {
    if (blobName == null || blobName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blob name cannot be empty");
    }
    if (blobName.length() > MAX_BLOB_NAME_LENGTH) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Blob name exceeds maximum length of " + MAX_BLOB_NAME_LENGTH + " characters");
    }
  }

  @Operation(
      summary = "Upload a document",
      description = "Uploads a file to Azure Blob Storage and creates metadata records "
          + "in the organization_document and organization_document_version tables. "
          + "Document type must be one of: policy, procedure, training_material, "
          + "certificate, attachment, report_export, other."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid file name, empty file, invalid input, or length exceeded"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden - user does not have access to organization"),
      @ApiResponse(responseCode = "500", description = "Storage or database error")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(
      @Parameter(description = "The file to upload", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = "Organization number", required = true)
      @RequestParam Integer orgNumber,

      @Parameter(description = "Document type (policy, procedure, training_material, " +
          "certificate, attachment, report_export, other)", required = true)
      @RequestParam String documentType,

      @Parameter(description = "Storage directory within the tenant's blob container")
      @RequestParam(defaultValue = "documents") String directory,

      HttpServletRequest request) throws IOException {

    Long userId = extractUserId(request);
    validateUserOrganizationAccess(userId, orgNumber);
    validateDocumentType(documentType);

    String originalFilename = file.getOriginalFilename();
    validateFilenameLength(originalFilename);

    String blobName = directory + "/" + originalFilename;
    validateBlobNameLength(blobName);

    OrganizationDocument doc = documentService.uploadDocument(orgNumber, file, documentType, directory);
    return ResponseEntity.ok(doc);
  }

  @Operation(
      summary = "Download a document",
      description = "Downloads the current version of a document by its ID. "
          + "Only returns documents belonging to the authenticated user's organization."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden - user does not have access to organization"),
      @ApiResponse(responseCode = "404", description = "Document not found")
  })
  @GetMapping("/download/{documentId}")
  public ResponseEntity<StreamingResponseBody> download(
      @Parameter(description = "ID of the document to download", required = true)
      @PathVariable Long documentId,

      @Parameter(description = "Organization number", required = true)
      @RequestParam Integer orgNumber,

      HttpServletRequest request) {

    Long userId = extractUserId(request);
    validateUserOrganizationAccess(userId, orgNumber);

    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

    OrganizationDocumentVersion version = versionRepo
        .findByDocumentIdAndVersionNumber(documentId, doc.getCurrentVersion())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document version not found"));

    String safeFilename = version.getOriginalFilename()
        .replaceAll("[^a-zA-Z0-9.\\-_ ]", "_");

    StreamingResponseBody responseBody = outputStream -> {
      try (InputStream inputStream = blobStorageService.downloadFile(orgNumber, version.getAzureBlobName())) {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
      } catch (IOException e) {
        throw new RuntimeException("Error streaming file", e);
      }
    };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(version.getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + safeFilename + "\"")
        .body(responseBody);
  }
}
