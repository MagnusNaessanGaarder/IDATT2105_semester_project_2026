package com.example.InternalControl.service.export;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.model.export.ExportType;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogEntryRepository;
import com.example.InternalControl.repository.training.TrainingRecordRepository;
import com.example.InternalControl.service.export.generator.PdfGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExportGeneratorService.
 */
@ExtendWith(MockitoExtension.class)
class ExportGeneratorServiceTest {

    @Mock
    private PdfGenerator pdfGenerator;

    @Mock
    private ChecklistRunRepository checklistRunRepository;

    @Mock
    private DeviationReportRepository deviationReportRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock

    private TemperatureLogEntryRepository temperatureLogEntryRepository;

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @InjectMocks
    private ExportGeneratorServiceImpl exportGeneratorService;

    private static final Integer ORG_NUMBER = 123;
    private static final Long JOB_ID = 1L;

    private ExportJob testJob;

    @BeforeEach
    void setUp() {
        testJob = createTestJob(JOB_ID, ExportType.CHECKLIST_REPORT, ExportFormat.PDF);
    }

    @Test
    void shouldGenerateChecklistPdfExport() {
        // Given
        ChecklistRun run = new ChecklistRun();
        run.setRunId(1L);

        when(checklistRunRepository.findByOrgNumberWithTemplate(ORG_NUMBER)).thenReturn(List.of(run));
        when(pdfGenerator.generateChecklistReport(anyList(), any(ExportJob.class)))
                .thenReturn("PDF content".getBytes());

        // When
        byte[] result = exportGeneratorService.generateExport(testJob);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("PDF content".getBytes());
        verify(pdfGenerator).generateChecklistReport(anyList(), eq(testJob));
    }

    @Test
    void shouldGenerateFullCompliancePdfExport() {
        // Given
        testJob.setExportType(ExportType.FULL_COMPLIANCE_REPORT);
        when(checklistRunRepository.findByOrgNumberWithTemplate(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(pdfGenerator.generateFullComplianceReport(anyMap(), any(ExportJob.class)))
                .thenReturn("PDF content".getBytes());

        // When
        byte[] result = exportGeneratorService.generateExport(testJob);

        // Then
        assertThat(result).isNotNull();
        verify(pdfGenerator).generateFullComplianceReport(anyMap(), eq(testJob));
    }

    // ==================== HELPER METHODS ====================

    private ExportJob createTestJob(Long jobId, ExportType type, ExportFormat format) {
        ExportJob job = new ExportJob();
        job.setExportJobId(jobId);
        job.setOrgNumber(ORG_NUMBER);
        job.setExportType(type);
        job.setFormat(format);
        job.setStatus(ExportStatus.PENDING);
        job.setParametersJson(null);
        return job;
    }
}
