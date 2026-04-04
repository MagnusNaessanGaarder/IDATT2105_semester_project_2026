package com.example.InternalControl.service.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.repository.export.ExportJobRepository;
import com.example.InternalControl.shared.enums.ExportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExportService.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

  @Mock
  private ExportJobRepository exportJobRepository;

  @Mock
  private ExportJobProcessor exportJobProcessor;

  @InjectMocks
  private ExportServiceImpl exportService;

  private ExportRequest validRequest;
  private ExportJob pendingJob;
  private ExportJob completedJob;

  @BeforeEach
  void setUp() {
    validRequest = ExportRequest.builder()
        .exportType(ExportType.checklist_report)
        .format(ExportFormat.pdf)
        .build();

    pendingJob = ExportJob.builder()
        .exportJobId(1L)
        .orgNumber(123456789)
        .requestedByUserId(1L)
        .exportType(ExportType.checklist_report)
        .format(ExportFormat.pdf)
        .status(ExportStatus.pending)
        .build();

    completedJob = ExportJob.builder()
        .exportJobId(2L)
        .orgNumber(123456789)
        .requestedByUserId(1L)
        .exportType(ExportType.checklist_report)
        .format(ExportFormat.pdf)
        .status(ExportStatus.completed)
        .resultDocumentId(100L)
        .build();
  }

  @Test
  void shouldCreateExportJob() {
    // Given
    when(exportJobRepository.save(any(ExportJob.class))).thenReturn(pendingJob);

    // When
    ExportResponse result = exportService.createExportJob(validRequest, 123456789, 1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getExportJobId()).isEqualTo(1L);
    assertThat(result.getStatus()).isEqualTo(ExportStatus.pending);
    verify(exportJobRepository).save(any(ExportJob.class));
    verify(exportJobProcessor).processExportJobAsync(1L);
  }

  @Test
  void shouldGetExportStatus() {
    // Given
    when(exportJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));

    // When
    ExportResponse result = exportService.getExportStatus(1L, 123456789);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getExportJobId()).isEqualTo(1L);
    assertThat(result.getStatus()).isEqualTo(ExportStatus.pending);
  }

  @Test
  void shouldThrowExceptionWhenExportNotFound() {
    // Given
    when(exportJobRepository.findById(99L)).thenReturn(Optional.empty());

    // Then
    assertThatThrownBy(() -> exportService.getExportStatus(99L, 123456789))
        .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
        .hasMessageContaining("Export job not found");
  }

  @Test
  void shouldThrowExceptionWhenAccessingOtherOrgExport() {
    // Given
    when(exportJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));

    // Then
    assertThatThrownBy(() -> exportService.getExportStatus(1L, 999999999))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("Access denied");
  }

  @Test
  void shouldThrowExceptionWhenDownloadingPendingExport() {
    // Given
    when(exportJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));

    // Then
    assertThatThrownBy(() -> exportService.getDownloadUrl(1L, 123456789))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Export not ready");
  }
}
