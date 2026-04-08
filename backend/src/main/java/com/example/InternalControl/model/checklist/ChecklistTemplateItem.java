package com.example.InternalControl.model.checklist;

import com.example.InternalControl.model.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Individual question/item in a checklist template.
 */
@Entity
@Table(name = "checklist_template_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnore
    private ChecklistTemplate template;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 15)
    private ItemType itemType;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = true;

    @Column(name = "expected_text", length = 255)
    private String expectedText;

    @Column(name = "expected_numeric_min", precision = 10, scale = 2)
    private BigDecimal expectedNumericMin;

    @Column(name = "expected_numeric_max", precision = 10, scale = 2)
    private BigDecimal expectedNumericMax;

    @Column(name = "choice_options_json", columnDefinition = "JSON")
    private String choiceOptionsJson;

    /**
     * Checks if this item has numeric range validation.
     */
    public boolean hasExpectedRange() {
        return expectedNumericMin != null && expectedNumericMax != null;
    }

    /**
     * Checks if this is a choice-type question.
     */
    public boolean isChoiceType() {
        return itemType == ItemType.CHOICE;
    }
}
