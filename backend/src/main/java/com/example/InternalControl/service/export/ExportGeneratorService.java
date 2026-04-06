package com.example.InternalControl.service.export;

import com.example.InternalControl.model.export.ExportJob;

/**
 * Service for generating export files (PDF/JSON).
 * Orchestrates data fetching and document generation.

 */
public interface ExportGeneratorService {

  /**
   * Generates the export file for a given job.
   * Fetches data based on export type and generates PDF or JSON.
   *
   * @param job the export job to process
   * @return the generated file content as byte array
   */
  byte[] generateExport(ExportJob job);
}
