package com.example.InternalControl.model.export;

import com.example.InternalControl.model.document.OrganizationDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an export job.
 */
@Entity
@Table(name = "export_job")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
  @Column(name = "export_type", nullable = false, length = 50)
  private ExportType exportType;

  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, length = 10)
  private ExportFormat format;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private ExportStatus status = ExportStatus.PENDING;

  @Column(name = "parameters_json", columnDefinition = "JSON")
  private String parametersJson;

  @Column(name = "result_document_id")
  private Long resultDocumentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "result_document_id", insertable = false, updatable = false)
  private OrganizationDocument resultDocument;

  @CreationTimestamp
  @Column(name = "requested_at", updatable = false)
  private LocalDateTime requestedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "failure_reason", columnDefinition = "TEXT")
  private String failureReason;

}
