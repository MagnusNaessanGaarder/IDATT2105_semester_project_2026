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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

@RestController
@RequestMapping("/api/files")
public class FileController {

  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepo;
  private final OrganizationDocumentVersionRepository versionRepo;

  public FileController(BlobStorageService blobStorageService,
                        OrganizationDocumentRepository documentRepo,
                        OrganizationDocumentVersionRepository versionRepo) {
    this.blobStorageService = blobStorageService;
    this.documentRepo = documentRepo;
    this.versionRepo = versionRepo;
  }

  @PostMapping("/upload")
  public ResponseEntity<?> upload(
      @RequestHeader("X-Org-Number") Integer orgNumber,  // Integer, not int
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "other") String documentType,
      @RequestParam(defaultValue = "documents") String directory) throws IOException {

    String blobName = blobStorageService.uploadFile(
        orgNumber, directory,
        file.getOriginalFilename(),
        file.getInputStream(),
        file.getSize(),
        file.getContentType()
    );

    OrganizationDocument doc = new OrganizationDocument();
    doc.setOrgNumber(orgNumber);
    doc.setDocumentType(documentType);
    doc.setTitle(file.getOriginalFilename());
    doc.setCurrentVersion(1);
    doc.setActive(true);
    documentRepo.save(doc);

    OrganizationDocumentVersion version = new OrganizationDocumentVersion();
    version.setDocumentId(doc.getDocumentId());
    version.setVersionNumber(1);
    version.setAzureContainer(blobStorageService.getContainerName(orgNumber));
    version.setAzureBlobName(blobName);
    version.setOriginalFilename(file.getOriginalFilename());
    version.setMimeType(file.getContentType());
    version.setFileSizeBytes(file.getSize());
    versionRepo.save(version);

    return ResponseEntity.ok(doc);
  }

  @GetMapping("/download/{documentId}")
  public ResponseEntity<byte[]> download(
      @RequestHeader("X-Org-Number") Integer orgNumber,  // Integer, not int
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