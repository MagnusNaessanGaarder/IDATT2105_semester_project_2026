package com.example.InternalControl.model.notification;

import com.example.InternalControl.shared.enums.TrainingType;
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
@Table(name = "training_record")
public class TrainingRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "training_record_id")
  private Long trainingRecordId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "training_type", nullable = false)
  private TrainingType trainingType;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TrainingStatus status;

  @Column(name = "certificate_document_id")
  private Long certificateDocumentId;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (status == null) {
      status = TrainingStatus.assigned;
    }
  }

  public enum TrainingStatus {
    assigned,
    completed,
    expired
  }
}
