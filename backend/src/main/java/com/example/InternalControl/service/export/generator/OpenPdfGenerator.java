package com.example.InternalControl.service.export.generator;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.training.TrainingRecord;
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
  public byte[] generateTemperatureReport(Map<String, Object> data, ExportJob job) {
    log.info("Generating temperature PDF for job {}", job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      @SuppressWarnings("unchecked")
      List<com.example.InternalControl.model.temperature.TemperatureLogEntry> entries =
              (List<com.example.InternalControl.model.temperature.TemperatureLogEntry>)
                      data.getOrDefault("temperatureLogs", List.of());

      addTitle(document, "Temperature Report", job);
      addSummary(document, entries.size(), job);

      if (!entries.isEmpty()) {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        long alertCount = entries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsAlert())).count();
        document.add(new Paragraph("Alerts: " + alertCount, boldFont));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 2, 2, 1, 3});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        for (String header : new String[]{"Measured At", "Log Point", "Temperature (°C)", "Alert", "Note"}) {
          PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
          cell.setBackgroundColor(Color.LIGHT_GRAY);
          table.addCell(cell);
        }

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (var entry : entries) {
          table.addCell(new Phrase(entry.getMeasuredAt().format(DATE_TIME_FORMATTER), cellFont));
          String logPointName = entry.getLogPoint() != null ? entry.getLogPoint().getName() : "—";
          table.addCell(new Phrase(logPointName, cellFont));
          table.addCell(new Phrase(entry.getTemperatureC().toPlainString(), cellFont));
          PdfPCell alertCell = new PdfPCell(new Phrase(Boolean.TRUE.equals(entry.getIsAlert()) ? "YES" : "No", cellFont));
          if (Boolean.TRUE.equals(entry.getIsAlert())) {
            alertCell.setBackgroundColor(new Color(255, 200, 200));
          }
          table.addCell(alertCell);
          table.addCell(new Phrase(entry.getNoteText() != null ? entry.getNoteText() : "", cellFont));
        }
        document.add(table);
      } else {
        document.add(new Paragraph("No temperature records found for the selected period."));
      }

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

      @SuppressWarnings("unchecked")
      List<com.example.InternalControl.model.training.TrainingRecord> records =
              (List<com.example.InternalControl.model.training.TrainingRecord>)
                      data.getOrDefault("trainingRecords", List.of());

      addTitle(document, "Training Report", job);
      addSummary(document, records.size(), job);

      if (!records.isEmpty()) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 3, 2, 2});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        for (String header : new String[]{"Title", "Type", "Status", "Completed At"}) {
          PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
          cell.setBackgroundColor(Color.LIGHT_GRAY);
          table.addCell(cell);
        }

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (var record : records) {
          table.addCell(new Phrase(record.getTitle(), cellFont));
          table.addCell(new Phrase(record.getTrainingType() != null ? record.getTrainingType().name() : "N/A", cellFont));
          table.addCell(new Phrase(record.getStatus() != null ? record.getStatus().name() : "N/A", cellFont));
          table.addCell(new Phrase(record.getCompletedAt() != null ? record.getCompletedAt().format(DATE_TIME_FORMATTER) : "—", cellFont));
        }
        document.add(table);
      } else {
        document.add(new Paragraph("No training records found."));
      }

      addFooter(document);
      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate training PDF for job {}", job.getExportJobId(), e);
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

      @SuppressWarnings("unchecked")
      List<ChecklistRun> runs = (List<ChecklistRun>) data.getOrDefault("checklistRuns", List.of());
      @SuppressWarnings("unchecked")
      List<DeviationReport> reports = (List<DeviationReport>) data.getOrDefault("deviationReports", List.of());
      @SuppressWarnings("unchecked")
      List<com.example.InternalControl.model.temperature.TemperatureLogEntry> entries =
              (List<com.example.InternalControl.model.temperature.TemperatureLogEntry>) data.getOrDefault("temperatureLogs", List.of());
      @SuppressWarnings("unchecked")
      List<com.example.InternalControl.model.training.TrainingRecord> trainingRecords =
              (List<com.example.InternalControl.model.training.TrainingRecord>) data.getOrDefault("trainingRecords", List.of());

      addTitle(document, "Full Compliance Report", job);

      Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
      Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
      document.add(new Paragraph("Summary:", boldFont));
      document.add(new Paragraph("Checklist Runs: " + runs.size(), normalFont));
      document.add(new Paragraph("Deviation Reports: " + reports.size(), normalFont));
      document.add(new Paragraph("Temperature Records: " + entries.size(), normalFont));
      document.add(new Paragraph("Training Records: " + trainingRecords.size(), normalFont));
      document.add(Chunk.NEWLINE);

      if (!runs.isEmpty()) {
        document.add(new Paragraph("Checklist Runs", boldFont));
        document.add(Chunk.NEWLINE);
        addChecklistTable(document, runs);
        document.add(Chunk.NEWLINE);
      }

      if (!reports.isEmpty()) {
        document.add(new Paragraph("Deviation Reports", boldFont));
        document.add(Chunk.NEWLINE);
        addDeviationTable(document, reports);
        document.add(Chunk.NEWLINE);
      }

      addFooter(document);
      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate compliance PDF for job {}", job.getExportJobId(), e);
      throw new RuntimeException("PDF generation failed", e);
    }
  }

  @Override
  public byte[] generateAuditReport(Map<String, Object> data, ExportJob job) {
    log.info("Generating audit PDF for job {}", job.getExportJobId());

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4);
      PdfWriter.getInstance(document, baos);
      document.open();

      @SuppressWarnings("unchecked")
      List<ChecklistRun> runs = (List<ChecklistRun>) data.getOrDefault("checklistRuns", List.of());
      @SuppressWarnings("unchecked")
      List<DeviationReport> reports = (List<DeviationReport>) data.getOrDefault("deviationReports", List.of());

      addTitle(document, "Audit Report", job);

      Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
      Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
      document.add(new Paragraph("Summary:", boldFont));
      document.add(new Paragraph("Checklist Runs Audited: " + runs.size(), normalFont));
      document.add(new Paragraph("Deviation Reports Audited: " + reports.size(), normalFont));
      document.add(Chunk.NEWLINE);

      if (!runs.isEmpty()) {
        document.add(new Paragraph("Checklist Runs", boldFont));
        document.add(Chunk.NEWLINE);
        addChecklistTable(document, runs);
        document.add(Chunk.NEWLINE);
      }

      if (!reports.isEmpty()) {
        document.add(new Paragraph("Deviation Reports", boldFont));
        document.add(Chunk.NEWLINE);
        addDeviationTable(document, reports);
        document.add(Chunk.NEWLINE);
      }

      addFooter(document);
      document.close();
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Failed to generate audit PDF for job {}", job.getExportJobId(), e);
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
    for (String header : new String[]{"Date", "Template", "Location", "Status", "Items"}) {
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
    for (String header : new String[]{"Date", "Title", "Severity", "Status", "Assigned To"}) {
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
