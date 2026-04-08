package com.example.InternalControl.model.deviation;

import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.ReportType;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Deviation/Incident report entity.
 */
@Entity
@Table(name = "deviation_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviationReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "report_id")
  private Long reportId;

  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_type", nullable = false, length = 20)
  private ReportType reportType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Severity severity;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id")
  @JsonIgnore
  private Location location;

  @Column(name = "location_text", length = 100)
  private String locationText;

  @Column(name = "occurred_date")
  private LocalDate occurredDate;

  @Column(name = "occurred_time")
  private LocalTime occurredTime;

  @Column(name = "report_date", nullable = false)
  private LocalDate reportDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_by_user_id", nullable = false)
  @JsonIgnore
  private AppUser reportedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "discovered_by_user_id")
  @JsonIgnore
  private AppUser discoveredBy;

  @Column(name = "discovered_by_name", length = 255)
  private String discoveredByName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_to_user_id")
  @JsonIgnore
  private AppUser reportedTo;

  @Column(name = "reported_to_name", length = 255)
  private String reportedToName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_user_id")
  @JsonIgnore
  private AppUser assignedTo;

  @Column(name = "immediate_action_text", columnDefinition = "TEXT")
  private String immediateActionText;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "immediate_action_signed_by_user_id")
  @JsonIgnore
  private AppUser immediateActionSignedBy;

  @Column(name = "cause_analysis_text", columnDefinition = "TEXT")
  private String causeAnalysisText;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cause_analysis_signed_by_user_id")
  @JsonIgnore
  private AppUser causeAnalysisSignedBy;

  @Column(name = "corrective_action_text", columnDefinition = "TEXT")
  private String correctiveActionText;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "corrective_action_signed_by_user_id")
  @JsonIgnore
  private AppUser correctiveActionSignedBy;

  @Column(name = "completion_text", columnDefinition = "TEXT")
  private String completionText;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "completion_signed_by_user_id")
  @JsonIgnore
  private AppUser completionSignedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private DeviationStatus status = DeviationStatus.REPORTED;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @ManyToMany
  @JoinTable(name = "deviation_report_document", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "document_id"))
  @JsonIgnore
  @Builder.Default
  @JsonIgnore
  private Set<OrganizationDocument> documents = new HashSet<>();

  public void addDocument(OrganizationDocument document) {
    documents.add(document);
  }

  public void removeDocument(OrganizationDocument document) {
    documents.remove(document);
  }

  public void close() {
    this.status = DeviationStatus.CLOSED;
    this.closedAt = LocalDateTime.now();
  }

  public boolean isEditable() {
    return status == DeviationStatus.DRAFT || status == DeviationStatus.REPORTED;
  }

  public boolean isClosed() {
    return status == DeviationStatus.CLOSED;
  }
}
