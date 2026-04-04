package com.example.InternalControl.service.export;

import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.repository.export.ExportJobRepository;
import com.example.InternalControl.service.BlobStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

/**
 * Implementation of ExportJobProcessor.
 * Handles async processing of export jobs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobProcessorImpl implements ExportJobProcessor {

  private final ExportJobRepository exportJobRepository;
  private final ExportGeneratorService exportGeneratorService;
  private final BlobStorageService blobStorageService;

  @Override
  @Async("exportTaskExecutor")
  @Transactional
  public void processExportJobAsync(Long exportJobId) {
    log.info("Starting async processing of export job {}", exportJobId);

    ExportJob job = exportJobRepository.findById(exportJobId)
        .orElseThrow(() -> new IllegalStateException("Export job not found: " + exportJobId));

    try {
      // Update status to running
      job.setStatus(ExportStatus.running);
      exportJobRepository.save(job);

      // Generate the export
      byte[] content = exportGeneratorService.generateExport(job);

      // Upload to blob storage
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

      // Update job as completed
      job.setStatus(ExportStatus.completed);
      job.setCompletedAt(LocalDateTime.now());
      exportJobRepository.save(job);

      log.info("Export job {} completed successfully. Blob: {}", exportJobId, blobName);

    } catch (Exception e) {
      log.error("Export job {} failed", exportJobId, e);

      job.setStatus(ExportStatus.failed);
      job.setFailureReason(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
      exportJobRepository.save(job);
    }
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
