package com.example.InternalControl.service.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for export operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface ExportService {

  /**
   * Creates a new export job.
   *
   * @param request the export request
   * @param orgNumber the organization number
   * @param userId the requesting user ID
   * @return the created export job response
   */
  ExportResponse createExportJob(ExportRequest request, Integer orgNumber, Long userId);

  /**
   * Gets the status of an export job.
   *
   * @param exportJobId the job ID
   * @param orgNumber the organization number
   * @return the export response
   */
  ExportResponse getExportStatus(Long exportJobId, Integer orgNumber);

  /**
   * Gets a presigned URL for downloading the export.
   *
   * @param exportJobId the job ID
   * @param orgNumber the organization number
   * @return the download URL
   */
  String getDownloadUrl(Long exportJobId, Integer orgNumber);

  /**
   * Lists all export jobs for an organization.
   *
   * @param orgNumber the organization number
   * @param pageable pagination info
   * @return page of export responses
   */
  Page<ExportResponse> listExports(Integer orgNumber, Pageable pageable);
}
