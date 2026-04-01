package com.example.InternalControl.model;

import com.example.InternalControl.model.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specific execution/instance of a checklist template.
 *
 * Database table: checklist_run (lines 334-358)
 * Primary key: run_id (BIGINT UNSIGNED, AUTO_INCREMENT)
 * Foreign keys:
 *   - template_id → checklist_template(template_id)
 *   - location_id → location(location_id)
 *
 * Business rules:
 * - Created by scheduler or manually
 * - Has status workflow (draft → in_progress → completed)
 * - Belongs to specific date
 * - Contains answers (ChecklistRunItem)
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "checklist_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    @Column(name = "org_number", nullable = false)
    private Integer orgNumber;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private RunStatus status = RunStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChecklistRunItem> items = new ArrayList<>();

    /**
     * Checks if this run can be edited.
     *
     * @return true if status is DRAFT or IN_PROGRESS
     */
    public boolean isEditable() {
        return status == RunStatus.DRAFT || status == RunStatus.IN_PROGRESS;
    }

    /**
     * Checks if run is overdue.
     *
     * @return true if past due date and not completed/cancelled
     */
    public boolean isOverdue() {
        return dueAt != null
               && LocalDateTime.now().isAfter(dueAt)
               && status != RunStatus.COMPLETED
               && status != RunStatus.CANCELLED;
    }

    /**
     * Marks run as completed.
     */
    public void markAsCompleted() {
        this.status = RunStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
