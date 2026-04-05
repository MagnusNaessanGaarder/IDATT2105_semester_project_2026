package com.example.InternalControl.service.export.generator;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.export.ExportJob;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PDF generator implementation using OpenPDF.
 */
@Component
@Slf4j
public class OpenPdfGenerator implements PdfGenerator {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  @Override
  public byte[] generateChecklistReport(List<ChecklistRun> runs, ExportJob job) {
    log.info("Generating checklist PDF with {} runs for job {}", runs.size(), job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      addTitle(document, "Checklist Report", job);
      addSummary(document, runs.size(), job);

      if (!runs.isEmpty()) {
        addChecklistTable(document, runs);
      } else {
        document.add(new Paragraph("No checklist runs found for the selected period."));
      }

      addFooter(document);
      document.close();

      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate checklist PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  @Override
  public byte[] generateDeviationReport(List<DeviationReport> reports, ExportJob job) {
    log.info("Generating deviation PDF with {} reports for job {}", reports.size(), job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      addTitle(document, "Deviation Report", job);
      addSummary(document, reports.size(), job);

      if (!reports.isEmpty()) {
        addDeviationTable(document, reports);
      } else {
        document.add(new Paragraph("No deviation reports found for the selected period."));
      }

      addFooter(document);
      document.close();

      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate deviation PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  @Override
  public byte[] generateFullComplianceReport(Map<String, Object> data, ExportJob job) {
    log.info("Generating full compliance PDF for job {}", job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      addTitle(document, "Full Compliance Report", job);
      addSummary(document, 0, job);

      document.add(new Paragraph("This is a comprehensive compliance report."));
      document.add(Chunk.NEWLINE);

      addFooter(document);
      document.close();

      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate compliance PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  private void addTitle(Document document, String title, ExportJob job) throws DocumentException {
    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Paragraph titlePara = new Paragraph(title, titleFont);
    titlePara.setAlignment(Element.ALIGN_CENTER);
    document.add(titlePara);
    document.add(Chunk.NEWLINE);

    Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
    String dateRange = job.getRequestedAt() != null
        ? job.getRequestedAt().format(DATE_FORMATTER)
        : "N/A";
    Paragraph subtitle = new Paragraph(
        String.format("Organization: %s | Generated: %s", job.getOrgNumber(), dateRange),
        subtitleFont
    );
    subtitle.setAlignment(Element.ALIGN_CENTER);
    document.add(subtitle);
    document.add(Chunk.NEWLINE);
  }

  private void addSummary(Document document, int count, ExportJob job) throws DocumentException {
    Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

    document.add(new Paragraph("Summary:", boldFont));
    document.add(new Paragraph("Total Records: " + count, normalFont));
    document.add(new Paragraph("Report Type: " + job.getExportType(), normalFont));
    document.add(new Paragraph("Format: " + job.getFormat(), normalFont));
    document.add(Chunk.NEWLINE);
  }

  private void addChecklistTable(Document document, List<ChecklistRun> runs) throws DocumentException {
    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{2, 3, 2, 2, 1});

    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    String[] headers = {"Date", "Template", "Location", "Status", "Items"};
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setBackgroundColor(Color.LIGHT_GRAY);
      table.addCell(cell);
    }

    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
    for (ChecklistRun run : runs) {
      table.addCell(new Phrase(run.getRunDate().format(DATE_FORMATTER), cellFont));
      table.addCell(new Phrase(run.getTemplate() != null ? run.getTemplate().getTitle() : "N/A", cellFont));
      table.addCell(new Phrase(run.getLocationId() != null ? "Location " + run.getLocationId() : "N/A", cellFont));
      table.addCell(new Phrase(run.getStatus().name(), cellFont));
      table.addCell(new Phrase(String.valueOf(run.getItems().size()), cellFont));
    }

    document.add(table);
  }

  private void addDeviationTable(Document document, List<DeviationReport> reports) throws DocumentException {
    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{2, 3, 2, 2, 2});

    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    String[] headers = {"Date", "Title", "Severity", "Status", "Assigned To"};
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setBackgroundColor(Color.LIGHT_GRAY);
      table.addCell(cell);
    }

    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
    for (DeviationReport report : reports) {
      table.addCell(new Phrase(report.getCreatedAt().format(DATE_FORMATTER), cellFont));
      table.addCell(new Phrase(report.getTitle(), cellFont));
      table.addCell(new Phrase(report.getSeverity().name(), cellFont));
      table.addCell(new Phrase(report.getStatus().name(), cellFont));
      table.addCell(new Phrase(report.getAssignedTo() != null ? report.getAssignedTo().toString() : "Unassigned", cellFont));
    }

    document.add(table);
  }

  @Override
  public byte[] generateAuditReport(Map<String, Object> data, ExportJob job) {
    log.info("Generating audit PDF for job {}", job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      addTitle(document, "Audit Report", job);
      addSummary(document, 0, job);

      document.add(new Paragraph("This report contains audit trail information."));
      document.add(Chunk.NEWLINE);

      Integer totalRuns = (Integer) data.getOrDefault("totalRuns", 0);
      Integer totalReports = (Integer) data.getOrDefault("totalReports", 0);
      document.add(new Paragraph("Checklist Runs Audited: " + totalRuns));
      document.add(new Paragraph("Deviation Reports Audited: " + totalReports));

      addFooter(document);
      document.close();

      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate audit PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  @Override
  public byte[] generateTemperatureReport(Map<String, Object> data, ExportJob job) {
    log.info("Generating temperature PDF for job {}", job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      addTitle(document, "Temperature Report", job);
      addSummary(document, 0, job);

      document.add(new Paragraph("This report contains temperature monitoring data."));
      document.add(Chunk.NEWLINE);

      document.add(new Paragraph("Temperature readings are recorded from checklist runs with temperature checks."));

      addFooter(document);
      document.close();

      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate temperature PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  @Override
  public byte[] generateTrainingReport(Map<String, Object> data, ExportJob job) {
    log.info("Generating training PDF for job {}", job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      addTitle(document, "Training Report", job);
      addSummary(document, 0, job);

      document.add(new Paragraph("This report contains training compliance information."));
      document.add(Chunk.NEWLINE);

      document.add(new Paragraph("Training records are tracked through deviation reports and checklist completions."));

      addFooter(document);
      document.close();

      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate training PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  private void addFooter(Document document) throws DocumentException {
    document.add(Chunk.NEWLINE);
    Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    Paragraph footer = new Paragraph(
        "Generated by Internal Control System | Confidential",
        footerFont
    );
    footer.setAlignment(Element.ALIGN_CENTER);
    document.add(footer);
  }
}
