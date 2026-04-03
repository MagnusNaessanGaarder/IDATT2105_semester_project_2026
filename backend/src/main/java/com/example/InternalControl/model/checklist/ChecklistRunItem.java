package com.example.InternalControl.model.checklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Answer to a specific question in a checklist run.
 *
 * Database table: checklist_run_item (lines 360-379)
 * Primary key: run_item_id (BIGINT UNSIGNED, AUTO_INCREMENT)
 * Foreign keys: run_id → checklist_run(run_id)
 *
 * Business rules:
 * - Links to template item (what was asked)
 * - Stores actual answer value
 * - Tracks deviations from expected values
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "checklist_run_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistRunItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_item_id")
    private Long runItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ChecklistRun run;

    @Column(name = "template_item_id", nullable = false)
    private Long templateItemId;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "numeric_value", precision = 10, scale = 2)
    private BigDecimal numericValue;

    @Column(name = "selected_choice", length = 255)
    private String selectedChoice;

    @Column(name = "is_deviation", nullable = false)
    @Builder.Default
    private Boolean isDeviation = false;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if this item has been answered.
     *
     * @return true if any value field is set
     */
    public boolean hasAnswer() {
        return booleanValue != null
               || textValue != null
               || numericValue != null
               || selectedChoice != null;
    }

    /**
     * Marks this item as having a deviation.
     *
     * @param reason explanation of the deviation
     */
    public void markAsDeviation(String reason) {
        this.isDeviation = true;
        this.commentText = reason;
    }
}
