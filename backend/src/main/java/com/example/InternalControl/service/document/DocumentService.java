package com.example.InternalControl.service.document;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.document.OrganizationDocumentVersion;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import com.example.InternalControl.service.storage.BlobStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Handles document uploads, versioning, and storage management.
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

  /**
   * Uploads {@code file} to blob storage and persists a brand-new
   * {@link OrganizationDocument} + its first {@link OrganizationDocumentVersion}.
   */
  @Transactional
  public OrganizationDocument uploadDocument(Integer orgNumber,
                                             MultipartFile file,
                                             String documentType,
                                             String title,
                                             String description,
                                             String directory) throws IOException {

    Fileparts fp = prepareFile(file, directory);
    String blobName = blobStorageService.uploadFile(
        orgNumber, fp.sanitizedDirectory(),
        fp.sanitizedFilename(),
        file.getInputStream(), file.getSize(), fp.contentType());

    try {
      OrganizationDocument doc = new OrganizationDocument();
      doc.setOrgNumber(orgNumber);
      doc.setDocumentType(documentType);
      doc.setTitle((title != null && !title.isBlank()) ? title : fp.originalFilename());
      doc.setDescription(description);
      doc.setCurrentVersion(1);
      doc.setActive(true);
      documentRepo.save(doc);

      saveVersion(doc.getDocumentId(), 1, orgNumber, blobName,
                  fp.originalFilename(), fp.contentType(), file.getSize());

      return doc;
    } catch (Exception e) {
      blobStorageService.deleteFile(orgNumber, blobName);
      throw e;
    }
  }

  /**
   * Uploads {@code file} as a new version of an existing document.
   * <p>
   * The parent document's {@code current_version} counter is incremented and
   * its optional {@code title} / {@code description} are updated when provided.
   * The old blob is left in place so previous versions remain downloadable if
   * a dedicated history endpoint is added later.
   *
   * @param orgNumber    tenant identifier — must match the document's org
   * @param documentId   ID of the document to version
   * @param file         the replacement file
   * @param title        optional updated title (null/blank = keep existing)
   * @param description  optional updated description (null = keep existing)
   * @param directory    blob sub-directory
   * @throws ResponseStatusException 404 if document not found, 409 if the
   *                                 next version number already exists
   */
  @Transactional
  public OrganizationDocument uploadNewVersion(Integer orgNumber,
                                               Long documentId,
                                               MultipartFile file,
                                               String title,
                                               String description,
                                               String directory) throws IOException {

    OrganizationDocument doc = documentRepo
        .findByDocumentIdAndOrgNumber(documentId, orgNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Document " + documentId + " not found in org " + orgNumber));

    int nextVersion = doc.getCurrentVersion() + 1;

    if (versionRepo.findByDocumentIdAndVersionNumber(documentId, nextVersion).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Version " + nextVersion + " already exists for document " + documentId);
    }

    Fileparts fp = prepareFile(file, directory);
    String blobName = blobStorageService.uploadFile(
        orgNumber, fp.sanitizedDirectory(),
        fp.sanitizedFilename(),
        file.getInputStream(), file.getSize(), fp.contentType());

    try {
      doc.setCurrentVersion(nextVersion);
      if (title != null && !title.isBlank())       doc.setTitle(title);
      if (description != null)                      doc.setDescription(description);
      documentRepo.save(doc);

      saveVersion(documentId, nextVersion, orgNumber, blobName,
                  fp.originalFilename(), fp.contentType(), file.getSize());

      return doc;
    } catch (Exception e) {
      blobStorageService.deleteFile(orgNumber, blobName);
      throw e;
    }
  }

  private void saveVersion(Long documentId, int versionNumber, int orgNumber,
                           String blobName, String originalFilename,
                           String contentType, long sizeBytes) {
    OrganizationDocumentVersion version = new OrganizationDocumentVersion();
    version.setDocumentId(documentId);
    version.setVersionNumber(versionNumber);
    version.setAzureContainer(blobStorageService.getContainerName(orgNumber));
    version.setAzureBlobName(blobName);
    version.setOriginalFilename(originalFilename);
    version.setMimeType(contentType);
    version.setFileSizeBytes(sizeBytes);
    versionRepo.save(version);
  }

  private static Fileparts prepareFile(MultipartFile file, String directory) {
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("File must have a name");
    }
    originalFilename = originalFilename.replaceAll(".*[/\\\\]", "");

    String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    if (sanitizedFilename.isBlank() || sanitizedFilename.startsWith(".")) {
      throw new IllegalArgumentException("Invalid file name");
    }

    if (file.isEmpty()) throw new IllegalArgumentException("File must not be empty");

    String sanitizedDirectory = directory.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    String contentType = file.getContentType();
    if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

    return new Fileparts(originalFilename, sanitizedFilename, sanitizedDirectory, contentType);
  }

  private record Fileparts(String originalFilename, String sanitizedFilename,
                           String sanitizedDirectory, String contentType) {}
}
