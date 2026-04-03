package com.example.InternalControl.controller.document;

import com.example.InternalControl.dto.document.DocumentDownloadDto;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.document.BlobStorageService;
import com.example.InternalControl.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller for file and document operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "Endpoints for uploading and downloading organization documents")
@RequiredArgsConstructor
public class FileController {

  private final DocumentService documentService;
  private final BlobStorageService blobStorageService;
  private final AuthenticationFacade authenticationFacade;

  @Operation(summary = "Upload a document", description = "Uploads a file to Azure Blob Storage and creates metadata records "
      + "in the organization_document and organization_document_version tables.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid file name, empty file, or invalid input"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "500", description = "Storage or database error")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<OrganizationDocument> upload(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestParam Integer orgNumber,

      @Parameter(description = "The file to upload", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = "Document type (e.g. policy, procedure, certificate, attachment, other)")
      @RequestParam(defaultValue = "other") String documentType,

      @Parameter(description = "Storage directory within the tenant's blob container")
      @RequestParam(defaultValue = "documents") String directory,

      HttpServletRequest request) throws IOException {

    authenticationFacade.extractAndValidateUser(request, orgNumber);
    OrganizationDocument doc = documentService.uploadDocument(orgNumber, file, documentType, directory);
    return ResponseEntity.ok(doc);
  }

  @Operation(summary = "Download a document", description = "Downloads the current version of a document by its ID. "
      + "Only returns documents belonging to the specified organization.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "404", description = "Document not found or does not belong to the organization")
  })
  @GetMapping("/download/{documentId}")
  public ResponseEntity<byte[]> download(
      @Parameter(description = "Organization number identifying the tenant", required = true)
      @RequestParam Integer orgNumber,

      @Parameter(description = "ID of the document to download", required = true)
      @PathVariable Long documentId,

      HttpServletRequest request) {

    authenticationFacade.extractAndValidateUser(request, orgNumber);

    DocumentDownloadDto downloadInfo = documentService.getDocumentForDownload(documentId, orgNumber);

    ByteArrayOutputStream stream = blobStorageService.downloadFile(orgNumber, downloadInfo.getAzureBlobName());

    String safeFilename = downloadInfo.getOriginalFilename()
        .replaceAll("[^a-zA-Z0-9.\\-_ ]", "_");

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(downloadInfo.getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + safeFilename + "\"")
        .body(stream.toByteArray());
  }
}
