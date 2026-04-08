package com.example.InternalControl.service.export;

/**
 * Processor for handling export jobs asynchronously.
 */
public interface ExportJobProcessor {

  void processExportJobAsync(Long exportJobId);
}
