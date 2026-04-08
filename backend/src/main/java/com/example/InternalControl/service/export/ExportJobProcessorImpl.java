package com.example.InternalControl.service.export;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.document.OrganizationDocumentVersion;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import com.example.InternalControl.repository.export.ExportJobRepository;
import com.example.InternalControl.service.storage.BlobStorageService;
import com.example.InternalControl.model.export.ExportFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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
  private final ExportJobStatusUpdater statusUpdater;

  @Override
  @Async("exportTaskExecutor")
  public void processExportJobAsync(Long exportJobId) {
    log.info("Starting async processing of export job {}", exportJobId);

    statusUpdater.setRunning(exportJobId);

    try {
      ExportJob job = exportJobRepository.findById(exportJobId)
              .orElseThrow(() -> new IllegalStateException("Export job not found: " + exportJobId));

      byte[] content = exportGeneratorService.generateExport(job);

      String fileName = generateFileName(job);
      String contentType = job.getFormat() == ExportFormat.PDF ? "application/pdf" : "application/json";
      String directory = "exports";

      String blobName = blobStorageService.uploadFile(
              job.getOrgNumber(), directory, fileName,
              new ByteArrayInputStream(content), content.length, contentType
      );

      OrganizationDocument document = createDocumentRecord(job, fileName, contentType, content.length, directory, blobName);

      statusUpdater.setCompleted(exportJobId, document.getDocumentId());
      log.info("Export job {} completed successfully.", exportJobId);

    } catch (Exception e) {
      log.error("Export job {} failed", exportJobId, e);
      statusUpdater.setFailed(exportJobId, e.getMessage());
    }
  }

  private OrganizationDocument createDocumentRecord(ExportJob job, String fileName, String contentType, long fileSize, String directory, String blobName) {
    OrganizationDocument document = new OrganizationDocument();
    document.setOrgNumber(job.getOrgNumber());
    document.setDocumentType("REPORT_EXPORT");
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
