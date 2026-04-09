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

  public Long getReportId() {
    return reportId;
  }

  public void setReportId(Long reportId) {
    this.reportId = reportId;
  }

  public Integer getOrgNumber() {
    return orgNumber;
  }

  public void setOrgNumber(Integer orgNumber) {
    this.orgNumber = orgNumber;
  }

  public ReportType getReportType() {
    return reportType;
  }

  public void setReportType(ReportType reportType) {
    this.reportType = reportType;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getLocationText() {
    return locationText;
  }

  public void setLocationText(String locationText) {
    this.locationText = locationText;
  }

  public LocalDate getOccurredDate() {
    return occurredDate;
  }

  public void setOccurredDate(LocalDate occurredDate) {
    this.occurredDate = occurredDate;
  }

  public LocalTime getOccurredTime() {
    return occurredTime;
  }

  public void setOccurredTime(LocalTime occurredTime) {
    this.occurredTime = occurredTime;
  }

  public LocalDate getReportDate() {
    return reportDate;
  }

  public void setReportDate(LocalDate reportDate) {
    this.reportDate = reportDate;
  }

  public AppUser getReportedBy() {
    return reportedBy;
  }

  public void setReportedBy(AppUser reportedBy) {
    this.reportedBy = reportedBy;
  }

  public AppUser getDiscoveredBy() {
    return discoveredBy;
  }

  public void setDiscoveredBy(AppUser discoveredBy) {
    this.discoveredBy = discoveredBy;
  }

  public String getDiscoveredByName() {
    return discoveredByName;
  }

  public void setDiscoveredByName(String discoveredByName) {
    this.discoveredByName = discoveredByName;
  }

  public AppUser getReportedTo() {
    return reportedTo;
  }

  public void setReportedTo(AppUser reportedTo) {
    this.reportedTo = reportedTo;
  }

  public String getReportedToName() {
    return reportedToName;
  }

  public void setReportedToName(String reportedToName) {
    this.reportedToName = reportedToName;
  }

  public AppUser getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(AppUser assignedTo) {
    this.assignedTo = assignedTo;
  }

  public String getImmediateActionText() {
    return immediateActionText;
  }

  public void setImmediateActionText(String immediateActionText) {
    this.immediateActionText = immediateActionText;
  }

  public AppUser getImmediateActionSignedBy() {
    return immediateActionSignedBy;
  }

  public void setImmediateActionSignedBy(AppUser immediateActionSignedBy) {
    this.immediateActionSignedBy = immediateActionSignedBy;
  }

  public String getCauseAnalysisText() {
    return causeAnalysisText;
  }

  public void setCauseAnalysisText(String causeAnalysisText) {
    this.causeAnalysisText = causeAnalysisText;
  }

  public AppUser getCauseAnalysisSignedBy() {
    return causeAnalysisSignedBy;
  }

  public void setCauseAnalysisSignedBy(AppUser causeAnalysisSignedBy) {
    this.causeAnalysisSignedBy = causeAnalysisSignedBy;
  }

  public String getCorrectiveActionText() {
    return correctiveActionText;
  }

  public void setCorrectiveActionText(String correctiveActionText) {
    this.correctiveActionText = correctiveActionText;
  }

  public AppUser getCorrectiveActionSignedBy() {
    return correctiveActionSignedBy;
  }

  public void setCorrectiveActionSignedBy(AppUser correctiveActionSignedBy) {
    this.correctiveActionSignedBy = correctiveActionSignedBy;
  }

  public String getCompletionText() {
    return completionText;
  }

  public void setCompletionText(String completionText) {
    this.completionText = completionText;
  }

  public AppUser getCompletionSignedBy() {
    return completionSignedBy;
  }

  public void setCompletionSignedBy(AppUser completionSignedBy) {
    this.completionSignedBy = completionSignedBy;
  }

  public DeviationStatus getStatus() {
    return status;
  }

  public void setStatus(DeviationStatus status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(LocalDateTime closedAt) {
    this.closedAt = closedAt;
  }

  public Set<OrganizationDocument> getDocuments() {
    return documents;
  }

  public void setDocuments(Set<OrganizationDocument> documents) {
    this.documents = documents;
  }
}
