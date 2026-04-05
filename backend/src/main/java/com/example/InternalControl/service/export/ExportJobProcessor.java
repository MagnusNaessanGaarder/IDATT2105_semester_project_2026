package com.example.InternalControl.service.export;

/**
 * Processor for handling export jobs asynchronously.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface ExportJobProcessor {

  /**
   * Processes an export job asynchronously.
   *
   * @param exportJobId the ID of the export job to process
   */
  void processExportJobAsync(Long exportJobId);
}
