package com.example.InternalControl.service.document;

import com.example.InternalControl.dto.document.DocumentDownloadDto;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.document.OrganizationDocumentVersion;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service for document operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
public class DocumentService {

  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepo;
  private final OrganizationDocumentVersionRepository versionRepo;

  public DocumentService(BlobStorageService blobStorageService,
      OrganizationDocumentRepository documentRepo,
      OrganizationDocumentVersionRepository versionRepo) {
    this.blobStorageService = blobStorageService;
    this.documentRepo = documentRepo;
    this.versionRepo = versionRepo;
  }

  @Transactional
  public OrganizationDocument uploadDocument(Integer orgNumber, MultipartFile file,
      String documentType, String directory) throws IOException {

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("File must have a name");
    }

    originalFilename = originalFilename.replaceAll(".*[/\\\\]", "");

    String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    if (sanitizedFilename.isBlank() || sanitizedFilename.startsWith(".")) {
      throw new IllegalArgumentException("Invalid file name");
    }

    String sanitizedDirectory = directory.replaceAll("[^a-zA-Z0-9\\-_]", "_");

    String contentType = file.getContentType();
    if (contentType == null || contentType.isBlank()) {
      contentType = "application/octet-stream";
    }

    if (file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty");
    }

    String blobName = blobStorageService.uploadFile(
        orgNumber, sanitizedDirectory,
        sanitizedFilename,
        file.getInputStream(),
        file.getSize(),
        contentType);

    try {
      OrganizationDocument doc = new OrganizationDocument();
      doc.setOrgNumber(orgNumber);
      doc.setDocumentType(documentType);
      doc.setTitle(originalFilename); // keep the human-readable original
      doc.setCurrentVersion(1);
      doc.setActive(true);
      documentRepo.save(doc);

      OrganizationDocumentVersion version = new OrganizationDocumentVersion();
      version.setDocumentId(doc.getDocumentId());
      version.setVersionNumber(1);
      version.setAzureContainer(blobStorageService.getContainerName(orgNumber));
      version.setAzureBlobName(blobName);
      version.setOriginalFilename(originalFilename);
      version.setMimeType(contentType);
      version.setFileSizeBytes(file.getSize());
      versionRepo.save(version);

      return doc;
    } catch (Exception e) {
      blobStorageService.deleteFile(orgNumber, blobName);
      throw e;
    }
  }

  @Transactional
  protected OrganizationDocument persistMetadata(Integer orgNumber, MultipartFile file,
      String documentType, String blobName) {
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

    return doc;
  }

  @Transactional(readOnly = true)
  public DocumentDownloadDto getDocumentForDownload(Long documentId, Integer orgNumber) {
    OrganizationDocument doc = documentRepo.findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new EntityNotFoundException("Document not found"));

    OrganizationDocumentVersion version = versionRepo
        .findByDocumentIdAndVersionNumber(documentId, doc.getCurrentVersion())
        .orElseThrow(() -> new EntityNotFoundException("Document version not found"));

    return DocumentDownloadDto.builder()
        .documentId(doc.getDocumentId())
        .originalFilename(version.getOriginalFilename())
        .mimeType(version.getMimeType())
        .azureBlobName(version.getAzureBlobName())
        .fileSizeBytes(version.getFileSizeBytes())
        .build();
  }
}
