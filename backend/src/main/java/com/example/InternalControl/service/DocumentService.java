package com.example.InternalControl.service;

import com.example.InternalControl.model.OrganizationDocument;
import com.example.InternalControl.model.OrganizationDocumentVersion;
import com.example.InternalControl.repository.OrganizationDocumentRepository;
import com.example.InternalControl.repository.OrganizationDocumentVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
    String blobName = blobStorageService.uploadFile(
        orgNumber, directory,
        file.getOriginalFilename(),
        file.getInputStream(),
        file.getSize(),
        file.getContentType()
    );

    try {
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
}