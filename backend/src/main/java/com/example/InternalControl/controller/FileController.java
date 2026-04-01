package com.example.InternalControl.controller;

import com.example.InternalControl.model.OrganizationDocument;
import com.example.InternalControl.model.OrganizationDocumentVersion;
import com.example.InternalControl.repository.OrganizationDocumentRepository;
import com.example.InternalControl.repository.OrganizationDocumentVersionRepository;
import com.example.InternalControl.service.BlobStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.InternalControl.service.DocumentService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

@RestController
@RequestMapping("/api/files")
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

  @PostMapping("/upload")
  public ResponseEntity<?> upload(
      @RequestHeader("X-Org-Number") Integer orgNumber,
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "other") String documentType,
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    OrganizationDocument doc = documentService.uploadDocument(orgNumber, file, documentType, directory);
    return ResponseEntity.ok(doc);
  }

  @GetMapping("/download/{documentId}")
  public ResponseEntity<byte[]> download(
      @RequestHeader("X-Org-Number") Integer orgNumber,
      @PathVariable Long documentId) {

    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    OrganizationDocumentVersion version = versionRepo
        .findByDocumentIdAndVersionNumber(documentId, doc.getCurrentVersion())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    ByteArrayOutputStream stream = blobStorageService.downloadFile(orgNumber, version.getAzureBlobName());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(version.getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + version.getOriginalFilename() + "\"")
        .body(stream.toByteArray());
  }
}