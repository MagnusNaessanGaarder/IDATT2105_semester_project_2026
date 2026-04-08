package com.example.InternalControl.service.export;

import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.repository.export.ExportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ExportJobStatusUpdater {

    private final ExportJobRepository exportJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setRunning(Long exportJobId) {
        ExportJob job = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalStateException("Export job not found: " + exportJobId));
        job.setStatus(ExportStatus.RUNNING);
        job.setFailureReason(null);
        job.setCompletedAt(null);
        job.setResultDocumentId(null);
        exportJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setCompleted(Long exportJobId, Long documentId) {
        ExportJob job = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalStateException("Export job not found: " + exportJobId));
        job.setStatus(ExportStatus.COMPLETED);
        job.setResultDocumentId(documentId);
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setFailed(Long exportJobId, String reason) {
        ExportJob job = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalStateException("Export job not found: " + exportJobId));
        job.setStatus(ExportStatus.FAILED);
        job.setFailureReason(reason);
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);
    }
}