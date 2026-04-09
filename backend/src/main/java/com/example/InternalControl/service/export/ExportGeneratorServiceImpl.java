package com.example.InternalControl.service.export;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.repository.temperature.TemperatureLogEntryRepository;
import com.example.InternalControl.repository.training.TrainingRecordRepository;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.service.export.generator.PdfGenerator;
import com.example.InternalControl.model.export.ExportType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportGeneratorServiceImpl implements ExportGeneratorService {

  private final PdfGenerator pdfGenerator;
  private final ChecklistRunRepository checklistRunRepository;
  private final DeviationReportRepository deviationReportRepository;
  private final ObjectMapper objectMapper;
  private final TemperatureLogEntryRepository temperatureLogEntryRepository;
  private final TrainingRecordRepository trainingRecordRepository;

  @Override
  public byte[] generateExport(ExportJob job) {
    log.info("Generating export for job {}: type={}, format={}",
        job.getExportJobId(), job.getExportType(), job.getFormat());

    ExportParameters params = parseParameters(job.getParametersJson());

    return switch (job.getFormat()) {
      case PDF -> generatePdfExport(job, params);
      case JSON -> generateJsonExport(job, params);
    };
  }

  private byte[] generatePdfExport(ExportJob job, ExportParameters params) {
    return switch (job.getExportType()) {
      case CHECKLIST_REPORT -> generateChecklistPdf(job, params);
      case DEVIATION_REPORT -> generateDeviationPdf(job, params);
      case FULL_COMPLIANCE_REPORT -> generateFullCompliancePdf(job, params);
      case AUDIT_REPORT -> generateAuditPdf(job, params);
      case TEMPERATURE_REPORT -> generateTemperaturePdf(job, params);
      case TRAINING_REPORT -> generateTrainingPdf(job, params);
    };
  }

  private byte[] generateJsonExport(ExportJob job, ExportParameters params) {
    Map<String, Object> data = fetchExportData(job, params);

    try {
      ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .enable(SerializationFeature.INDENT_OUTPUT);

      Map<String, Object> wrapper = new HashMap<>();
      wrapper.put("exportType", job.getExportType());
      wrapper.put("generatedAt", LocalDateTime.now());
      wrapper.put("orgNumber", job.getOrgNumber());
      wrapper.put("data", data);

      return mapper.writeValueAsBytes(wrapper);
    } catch (Exception e) {
      log.error("JSON serialization failed for job {}", job.getExportJobId(), e);
      throw new RuntimeException("JSON generation failed", e);
    }
  }

  private byte[] generateChecklistPdf(ExportJob job, ExportParameters params) {
    List<ChecklistRun> runs;

    if (params.dateFrom() != null && params.dateTo() != null) {
      runs = checklistRunRepository.findByOrgNumberAndRunDateBetweenWithTemplate(
          job.getOrgNumber(), params.dateFrom(), params.dateTo());
    } else {
      runs = checklistRunRepository.findByOrgNumberWithTemplate(job.getOrgNumber());
    }

    log.info("Found {} checklist runs for PDF export", runs.size());
    return pdfGenerator.generateChecklistReport(runs, job);
  }

  private byte[] generateDeviationPdf(ExportJob job, ExportParameters params) {
    List<DeviationReport> reports = deviationReportRepository.searchReports(
        job.getOrgNumber(),
        null,
        null,
        null,
        params.dateFrom(),
        params.dateTo()
    );

    log.info("Found {} deviation reports for PDF export", reports.size());
    return pdfGenerator.generateDeviationReport(reports, job);
  }

  private byte[] generateFullCompliancePdf(ExportJob job, ExportParameters params) {
    Map<String, Object> data = fetchExportData(job, params);
    return pdfGenerator.generateFullComplianceReport(data, job);
  }

  private byte[] generateAuditPdf(ExportJob job, ExportParameters params) {
    Map<String, Object> data = fetchExportData(job, params);
    return pdfGenerator.generateAuditReport(data, job);
  }

  private byte[] generateTemperaturePdf(ExportJob job, ExportParameters params) {
    Map<String, Object> data = fetchExportData(job, params);
    return pdfGenerator.generateTemperatureReport(data, job);
  }

  private byte[] generateTrainingPdf(ExportJob job, ExportParameters params) {
    Map<String, Object> data = fetchExportData(job, params);
    return pdfGenerator.generateTrainingReport(data, job);
  }

  private Map<String, Object> fetchExportData(ExportJob job, ExportParameters params) {
    Map<String, Object> data = new HashMap<>();

    switch (job.getExportType()) {
      case CHECKLIST_REPORT -> {
        List<ChecklistRun> runs = params.dateFrom() != null && params.dateTo() != null
            ? checklistRunRepository.findByOrgNumberAndRunDateBetween(
                job.getOrgNumber(), params.dateFrom(), params.dateTo())
            : checklistRunRepository.findByOrgNumber(job.getOrgNumber());
        data.put("checklistRuns", runs);
        data.put("totalRuns", runs.size());
      }
      case DEVIATION_REPORT -> {
        List<DeviationReport> reports = deviationReportRepository.searchReports(
            job.getOrgNumber(), null, null, null, params.dateFrom(), params.dateTo());
        data.put("deviationReports", reports);
        data.put("totalReports", reports.size());
      }
      case TEMPERATURE_REPORT -> {
        List<TemperatureLogEntry> entries = temperatureLogEntryRepository
                .findByOrgNumberWithLogPointOrderByMeasuredAtDesc(job.getOrgNumber());
        long alertCount = entries.stream().filter(e -> Boolean.TRUE.equals(e.getIsAlert())).count();
        data.put("temperatureLogs", entries);
        data.put("totalRecords", entries.size());
        data.put("totalAlerts", alertCount);
      }
      case TRAINING_REPORT -> {
        List<TrainingRecord> records = trainingRecordRepository
                .findByOrgNumber(job.getOrgNumber());
        data.put("trainingRecords", records);
        data.put("totalRecords", records.size());
      }
      case FULL_COMPLIANCE_REPORT, AUDIT_REPORT -> {
        List<ChecklistRun> runs = checklistRunRepository.findByOrgNumberWithTemplate(job.getOrgNumber());
        List<DeviationReport> reports = deviationReportRepository.findByOrgNumber(job.getOrgNumber());
        List<TemperatureLogEntry> entries = temperatureLogEntryRepository
                .findByOrgNumberOrderByMeasuredAtDesc(job.getOrgNumber());
        long alertCount = entries.stream().filter(e -> Boolean.TRUE.equals(e.getIsAlert())).count();
        List<TrainingRecord> records = trainingRecordRepository.findByOrgNumber(job.getOrgNumber());
        data.put("checklistRuns", runs);
        data.put("deviationReports", reports);
        data.put("temperatureLogs", entries);
        data.put("trainingRecords", records);
        data.put("totalRuns", runs.size());
        data.put("totalReports", reports.size());
        data.put("totalRecords", entries.size());
        data.put("alertCount", alertCount);
        entries.stream().filter(e -> Boolean.TRUE.equals(e.getIsAlert())).count();
      }
    }

    return data;
  }

  private ExportParameters parseParameters(String json) {
    if (json == null || json.isBlank()) {
      return new ExportParameters(null, null, null, null);
    }

    try {
      Map<String, Object> map = objectMapper.readValue(json, Map.class);
      LocalDate dateFrom = parseDate(map.get("dateFrom"));
      LocalDate dateTo = parseDate(map.get("dateTo"));
      Long locationId = map.get("locationId") != null
          ? Long.valueOf(map.get("locationId").toString()) : null;
      String checklistType = map.get("checklistType") != null
          ? map.get("checklistType").toString() : null;

      return new ExportParameters(dateFrom, dateTo, locationId, checklistType);
    } catch (Exception e) {
      log.warn("Failed to parse export parameters: {}", json, e);
      return new ExportParameters(null, null, null, null);
    }
  }

  private LocalDate parseDate(Object value) {
    if (value == null) return null;
    try {
      return LocalDate.parse(value.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private record ExportParameters(
      LocalDate dateFrom,
      LocalDate dateTo,
      Long locationId,
      String checklistType
  ) {}
}
