package com.example.InternalControl.service.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.model.export.ExportType;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.export.ExportJobRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

  private final ExportJobRepository exportJobRepository;
  private final ExportJobProcessor exportJobProcessor;
  private final ObjectMapper objectMapper;
  private final AppUserRepository appUserRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ExportResponse createExportJob(ExportRequest request, Integer orgNumber, Long userId) {
    log.info("Creating export job for org {}: type={}, format={}",
            orgNumber, request.getExportType(), request.getFormat());

    String parametersJson = serializeParameters(request);

    ExportJob job = ExportJob.builder()
            .orgNumber(orgNumber)
            .requestedByUserId(userId)
          .exportType(com.example.InternalControl.shared.enums.ExportType.valueOf(request.getExportType().name()))
            .format(request.getFormat())
            .status(ExportStatus.PENDING)
            .parametersJson(parametersJson)
            .build();

    ExportJob saved = exportJobRepository.save(job);

    // Trigger async processing after transaction commits to avoid race condition
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          exportJobProcessor.processExportJobAsync(saved.getExportJobId());
        }
      });
    } else {
      exportJobProcessor.processExportJobAsync(saved.getExportJobId());
    }

    return mapToResponse(saved);
  }

  private String serializeParameters(ExportRequest request) {
    try {
      Map<String, Object> params = new HashMap<>();
      if (request.getDateFrom() != null)     params.put("dateFrom", request.getDateFrom().toString());
      if (request.getDateTo() != null)       params.put("dateTo", request.getDateTo().toString());
      if (request.getLocationId() != null)   params.put("locationId", request.getLocationId());
      if (request.getChecklistType() != null) params.put("checklistType", request.getChecklistType());
      return objectMapper.writeValueAsString(params);
    } catch (Exception e) {
      log.warn("Failed to serialize export parameters", e);
      return null;
    }
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
  @Transactional(readOnly = true)
  public String getDownloadUrl(Long exportJobId, Integer orgNumber) {
    ExportJob job = exportJobRepository.findById(exportJobId)
            .orElseThrow(() -> new EntityNotFoundException("Export job not found: " + exportJobId));

    if (!job.getOrgNumber().equals(orgNumber)) {
      throw new AccessDeniedException("Access denied to export job");
    }

    if (job.getStatus() != ExportStatus.COMPLETED) {
      throw new IllegalStateException("Export not ready. Status: " + job.getStatus());
    }

    if (job.getResultDocumentId() == null) {
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
    String displayName = resolveDisplayName(job.getRequestedByUserId());

    return ExportResponse.builder()
            .exportJobId(job.getExportJobId())
          .exportType(ExportType.valueOf(job.getExportType().name()))
            .format(job.getFormat())
            .status(job.getStatus())
            .fileName(job.getResultDocument() != null ? job.getResultDocument().getTitle() : null)
            .failureReason(job.getFailureReason())
            .requestedAt(job.getRequestedAt())
            .completedAt(job.getCompletedAt())
            .requestedByDisplayName(displayName)
            .parametersJson(job.getParametersJson())
            .build();
  }

  /**
   * Resolves a user ID to a display name. Returns null gracefully if the user
   * no longer exists (e.g. was deleted) so the rest of the response is unaffected.
   */
  private String resolveDisplayName(Long userId) {
    if (userId == null) return null;
    return appUserRepository.findById(userId)
            .map(AppUser::getDisplayName)
            .orElse(null);
  }

  private String generateDownloadUrl(ExportJob job) {
    return "/api/files/download/" + job.getResultDocumentId();
  }
}