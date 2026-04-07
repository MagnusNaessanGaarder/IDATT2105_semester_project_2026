package com.example.InternalControl.model.training;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.user.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping to training_record table.
 * Tracks employee training and certifications.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "training_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "training_record_id")
    private Long trainingRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "org_number", nullable = false)
    private Integer orgNumber;

    @Column(name = "training_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TrainingType trainingType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrainingStatus status = TrainingStatus.ASSIGNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_document_id")
    private OrganizationDocument certificateDocument;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}