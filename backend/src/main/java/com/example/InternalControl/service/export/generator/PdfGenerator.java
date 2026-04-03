package com.example.InternalControl.service.export.generator;

import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.DeviationReport;
import com.example.InternalControl.model.export.ExportJob;

import java.util.List;
import java.util.Map;

/**
 * Generator for PDF reports.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface PdfGenerator {

  /**
   * Generates a checklist report PDF.
   *
   * @param runs the checklist runs
   * @param job the export job
   * @return PDF as byte array
   */
  byte[] generateChecklistReport(List<ChecklistRun> runs, ExportJob job);

  /**
   * Generates a deviation report PDF.
   *
   * @param reports the deviation reports
   * @param job the export job
   * @return PDF as byte array
   */
  byte[] generateDeviationReport(List<DeviationReport> reports, ExportJob job);

  /**
   * Generates a full compliance report PDF.
   *
   * @param data the combined data
   * @param job the export job
   * @return PDF as byte array
   */
  byte[] generateFullComplianceReport(Map<String, Object> data, ExportJob job);
}
