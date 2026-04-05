package com.example.InternalControl.service.export.generator;

import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.DeviationReport;
import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.shared.enums.ExportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OpenPdfGenerator.
 *
 * @author TriTacLe
 * @since 1.0
 */
class OpenPdfGeneratorTest {

  private OpenPdfGenerator pdfGenerator;
  private ExportJob mockJob;

  @BeforeEach
  void setUp() {
    pdfGenerator = new OpenPdfGenerator();
    mockJob = ExportJob.builder()
        .exportJobId(1L)
        .orgNumber(123456789)
        .exportType(ExportType.checklist_report)
        .format(ExportFormat.pdf)
        .status(ExportStatus.completed)
        .requestedByUserId(1L)
        .requestedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void generateChecklistReport_WithEmptyList_ReturnsPdf() {
    byte[] pdf = pdfGenerator.generateChecklistReport(Collections.emptyList(), mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
    // PDF files start with %PDF
    assertEquals('%', (char) pdf[0]);
    assertEquals('P', (char) pdf[1]);
    assertEquals('D', (char) pdf[2]);
    assertEquals('F', (char) pdf[3]);
  }

  @Test
  void generateDeviationReport_WithEmptyList_ReturnsPdf() {
    byte[] pdf = pdfGenerator.generateDeviationReport(Collections.emptyList(), mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
    assertEquals('%', (char) pdf[0]);
    assertEquals('P', (char) pdf[1]);
  }

  @Test
  void generateFullComplianceReport_WithEmptyData_ReturnsPdf() {
    Map<String, Object> data = new HashMap<>();
    data.put("totalRuns", 0);
    data.put("totalReports", 0);

    byte[] pdf = pdfGenerator.generateFullComplianceReport(data, mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
    assertEquals('%', (char) pdf[0]);
  }

  @Test
  void generateChecklistReport_WithSampleData_ReturnsValidPdf() {
    ChecklistRun run = ChecklistRun.builder()
        .runId(1L)
        .orgNumber(123456789)
        .runDate(LocalDate.now())
        .build();

    byte[] pdf = pdfGenerator.generateChecklistReport(Collections.singletonList(run), mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void generateAuditReport_ReturnsValidPdf() {
    Map<String, Object> data = new HashMap<>();
    data.put("totalRuns", 10);
    data.put("totalReports", 5);

    byte[] pdf = pdfGenerator.generateAuditReport(data, mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
    assertEquals('%', (char) pdf[0]);
  }

  @Test
  void generateTemperatureReport_ReturnsValidPdf() {
    Map<String, Object> data = new HashMap<>();

    byte[] pdf = pdfGenerator.generateTemperatureReport(data, mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
    assertEquals('%', (char) pdf[0]);
  }

  @Test
  void generateTrainingReport_ReturnsValidPdf() {
    Map<String, Object> data = new HashMap<>();

    byte[] pdf = pdfGenerator.generateTrainingReport(data, mockJob);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
    assertEquals('%', (char) pdf[0]);
  }
}
