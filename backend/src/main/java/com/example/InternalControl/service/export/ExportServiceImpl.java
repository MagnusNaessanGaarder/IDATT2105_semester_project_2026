package com.example.InternalControl.service.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.repository.export.ExportJobRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of ExportService.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportServiceImpl implements ExportService {

  private final ExportJobRepository exportJobRepository;
  private final ExportJobProcessor exportJobProcessor;

  @Override
  public ExportResponse createExportJob(ExportRequest request, Integer orgNumber, Long userId) {
    log.info("Creating export job for org {}: type={}, format={}",
        orgNumber, request.getExportType(), request.getFormat());

    ExportJob job = ExportJob.builder()
        .orgNumber(orgNumber)
        .requestedByUserId(userId)
        .exportType(request.getExportType())
        .format(request.getFormat())
        .status(ExportStatus.pending)
        .build();

    ExportJob saved = exportJobRepository.save(job);

    exportJobProcessor.processExportJobAsync(saved.getExportJobId());

    return mapToResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ExportResponse getExportStatus(Long exportJobId, Integer orgNumber) {
    ExportJob job = exportJobRepository.findById(exportJobId)
        .orElseThrow(() -> new EntityNotFoundException("Export job not found: " + exportJobId));

    if (!job.getOrgNumber().equals(orgNumber)) {
      throw new AccessDeniedException("Access denied to export job");
    }

    return mapToResponse(job);
  }

  @Override
  public String getDownloadUrl(Long exportJobId, Integer orgNumber) {
    ExportJob job = exportJobRepository.findById(exportJobId)
        .orElseThrow(() -> new EntityNotFoundException("Export job not found: " + exportJobId));

    if (!job.getOrgNumber().equals(orgNumber)) {
      throw new AccessDeniedException("Access denied to export job");
    }

    if (job.getStatus() != ExportStatus.completed) {
      throw new IllegalStateException("Export not ready. Status: " + job.getStatus());
    }

    if (job.getResultDocument() == null) {
      throw new IllegalStateException("Export file not available");
    }

    return generateDownloadUrl(job);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ExportResponse> listExports(Integer orgNumber, Pageable pageable) {
    return exportJobRepository.findByOrgNumber(orgNumber, pageable)
        .map(this::mapToResponse);
  }

  private ExportResponse mapToResponse(ExportJob job) {
    return ExportResponse.builder()
        .exportJobId(job.getExportJobId())
        .exportType(job.getExportType())
        .format(job.getFormat())
        .status(job.getStatus())
        .fileName(job.getResultDocument() != null ? job.getResultDocument().getTitle() : null)
        .failureReason(job.getFailureReason())
        .requestedAt(job.getRequestedAt())
        .completedAt(job.getCompletedAt())
        .build();
  }

  private String generateDownloadUrl(ExportJob job) {
    return "/api/documents/" + job.getResultDocumentId() + "/download";
  }
}
