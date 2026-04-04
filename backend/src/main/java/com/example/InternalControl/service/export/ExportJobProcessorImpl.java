package com.example.InternalControl.service.export;

import com.example.InternalControl.model.OrganizationDocument;
import com.example.InternalControl.model.OrganizationDocumentVersion;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.repository.OrganizationDocumentRepository;
import com.example.InternalControl.repository.OrganizationDocumentVersionRepository;
import com.example.InternalControl.repository.export.ExportJobRepository;
import com.example.InternalControl.service.BlobStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobProcessorImpl implements ExportJobProcessor {

  private final ExportJobRepository exportJobRepository;
  private final ExportGeneratorService exportGeneratorService;
  private final BlobStorageService blobStorageService;
  private final OrganizationDocumentRepository documentRepository;
  private final OrganizationDocumentVersionRepository versionRepository;

  @Override
  @Async("exportTaskExecutor")
  @Transactional
  public void processExportJobAsync(Long exportJobId) {
    log.info("Starting async processing of export job {}", exportJobId);

    ExportJob job = exportJobRepository.findById(exportJobId)
        .orElseThrow(() -> new IllegalStateException("Export job not found: " + exportJobId));

    try {
      job.setStatus(ExportStatus.running);
      exportJobRepository.save(job);

      byte[] content = exportGeneratorService.generateExport(job);

      String fileName = generateFileName(job);
      String contentType = job.getFormat().name().equals("pdf") ? "application/pdf" : "application/json";
      String directory = "exports";

      String blobName = blobStorageService.uploadFile(
          job.getOrgNumber(),
          directory,
          fileName,
          new ByteArrayInputStream(content),
          content.length,
          contentType
      );

      OrganizationDocument document = createDocumentRecord(job, fileName, contentType, content.length, directory, blobName);

      job.setStatus(ExportStatus.completed);
      job.setResultDocumentId(document.getDocumentId());
      job.setCompletedAt(LocalDateTime.now());
      exportJobRepository.save(job);

      log.info("Export job {} completed successfully. Document ID: {}", exportJobId, document.getDocumentId());

    } catch (Exception e) {
      log.error("Export job {} failed", exportJobId, e);

      job.setStatus(ExportStatus.failed);
      job.setFailureReason(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
      exportJobRepository.save(job);
    }
  }

  private OrganizationDocument createDocumentRecord(ExportJob job, String fileName, String contentType, long fileSize, String directory, String blobName) {
    OrganizationDocument document = new OrganizationDocument();
    document.setOrgNumber(job.getOrgNumber());
    document.setDocumentType("EXPORT");
    document.setTitle(fileName);
    document.setDescription("Export: " + job.getExportType() + " in " + job.getFormat() + " format");
    document.setCreatedByUserId(job.getRequestedByUserId());
    document.setActive(true);
    document.setCurrentVersion(1);

    OrganizationDocument savedDocument = documentRepository.save(document);

    OrganizationDocumentVersion version = new OrganizationDocumentVersion();
    version.setDocumentId(savedDocument.getDocumentId());
    version.setVersionNumber(1);
    version.setAzureContainer("org-" + job.getOrgNumber());
    version.setAzureBlobName(directory + "/" + fileName);
    version.setOriginalFilename(fileName);
    version.setMimeType(contentType);
    version.setFileSizeBytes(fileSize);
    version.setUploadedByUserId(job.getRequestedByUserId());

    versionRepository.save(version);

    return savedDocument;
  }

  private String generateFileName(ExportJob job) {
    String timestamp = LocalDateTime.now().toString().replace(":", "-");
    String extension = job.getFormat().name().toLowerCase();
    return String.format("%s_%s_%s.%s",
        job.getExportType(),
        job.getOrgNumber(),
        timestamp,
        extension
    );
  }
}
