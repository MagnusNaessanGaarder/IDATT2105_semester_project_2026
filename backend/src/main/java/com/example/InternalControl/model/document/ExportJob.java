package com.example.InternalControl.model.document;

import com.example.InternalControl.shared.enums.ExportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "export_job")
public class ExportJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "export_job_id")
  private Long exportJobId;

  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Column(name = "requested_by_user_id", nullable = false)
  private Long requestedByUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "export_type", nullable = false)
  private ExportType exportType;

  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, length = 20)
  private ExportFormat format;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ExportStatus status = ExportStatus.pending;

  @Column(name = "parameters_json", columnDefinition = "JSON")
  private String parametersJson;

  @Column(name = "result_document_id")
  private Long resultDocumentId;

  @Column(name = "requested_at", updatable = false)
  private LocalDateTime requestedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "failure_reason", columnDefinition = "TEXT")
  private String failureReason;

  @PrePersist
  protected void onCreate() {
    requestedAt = LocalDateTime.now();
  }

  public enum ExportFormat {
    pdf,
    json
  }

  public enum ExportStatus {
    pending,
    running,
    completed,
    failed
  }
}
