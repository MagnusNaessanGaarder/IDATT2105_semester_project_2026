package com.example.InternalControl.model.training;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "training_record", indexes = {
    @Index(name = "ix_training_org", columnList = "org_number"),
    @Index(name = "ix_training_user", columnList = "user_id"),
    @Index(name = "ix_training_status", columnList = "org_number, status"),
    @Index(name = "ix_training_expires", columnList = "expires_at")
})
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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TrainingStatus status = TrainingStatus.ASSIGNED;

    @Column(name = "certificate_document_id")
    private Long certificateDocumentId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
