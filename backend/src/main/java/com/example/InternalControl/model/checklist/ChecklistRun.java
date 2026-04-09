package com.example.InternalControl.model.checklist;

import com.example.InternalControl.model.enums.RunStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
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

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ChecklistRunItem> items = new ArrayList<>();

    /**
     * Checks if this run can be edited.
     */
    public boolean isEditable() {
        return status == RunStatus.DRAFT || status == RunStatus.IN_PROGRESS || status == RunStatus.OVERDUE;
    }

    /**
     * Checks if run is overdue.
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
