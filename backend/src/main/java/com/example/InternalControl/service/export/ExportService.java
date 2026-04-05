package com.example.InternalControl.service.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for export operations.
 */
public interface ExportService {

  ExportResponse createExportJob(ExportRequest request, Integer orgNumber, Long userId);

  ExportResponse getExportStatus(Long exportJobId, Integer orgNumber);

  String getDownloadUrl(Long exportJobId, Integer orgNumber);

  Page<ExportResponse> listExports(Integer orgNumber, Pageable pageable);
}
