package com.example.InternalControl.model;

import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Template defining what should be checked and how often.
 *
 * Database table: checklist_template (lines 295-313)
 * Primary key: template_id (BIGINT UNSIGNED, AUTO_INCREMENT)
 * Foreign keys: org_number → organization(org_number)
 *
 * Business rules:
 * - Belongs to one organization
 * - Can be activated/deactivated
 * - Has many items (questions)
 * - Used by scheduler to generate runs
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "checklist_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "org_number", nullable = false)
    private Integer orgNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false, length = 10)
    private ModuleType moduleType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequency frequency;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChecklistTemplateItem> items = new ArrayList<>();

    /**
     * Adds item and maintains bidirectional relationship.
     *
     * @param item the item to add
     */
    public void addItem(ChecklistTemplateItem item) {
        items.add(item);
        item.setTemplate(this);
    }

    /**
     * Removes item and breaks relationship.
     *
     * @param item the item to remove
     */
    public void removeItem(ChecklistTemplateItem item) {
        items.remove(item);
        item.setTemplate(null);
    }

    /**
     * Deactivates template (soft delete).
     */
    public void deactivate() {
        this.isActive = false;
    }
}
