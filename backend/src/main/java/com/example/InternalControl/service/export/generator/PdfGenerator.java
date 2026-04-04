package com.example.InternalControl.service.export.generator;

import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.DeviationReport;
import com.example.InternalControl.model.export.ExportJob;

import java.util.List;
import java.util.Map;

public interface PdfGenerator {

  byte[] generateChecklistReport(List<ChecklistRun> runs, ExportJob job);

  byte[] generateDeviationReport(List<DeviationReport> reports, ExportJob job);

  byte[] generateFullComplianceReport(Map<String, Object> data, ExportJob job);

  byte[] generateAuditReport(Map<String, Object> data, ExportJob job);

  byte[] generateTemperatureReport(Map<String, Object> data, ExportJob job);

  byte[] generateTrainingReport(Map<String, Object> data, ExportJob job);
}
