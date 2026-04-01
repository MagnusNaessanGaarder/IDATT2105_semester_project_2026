package com.example.InternalControl.model;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_document")
public class OrganizationDocument {

  @Setter
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "document_id")
  private Long documentId;

  @Setter
  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Setter
  @Column(name = "document_type", nullable = false)
  private String documentType;

  @Setter
  @Column(name = "title", nullable = false)
  private String title;

  @Setter
  @Column(name = "description")
  private String description;

  @Setter
  @Column(name = "current_version", nullable = false)
  private Integer currentVersion = 1;

  @Setter
  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Setter
  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }


  public Long getDocumentId() { return documentId; }

  public Integer getOrgNumber() { return orgNumber; }

  public String getDocumentType() { return documentType; }

  public String getTitle() { return title; }

  public String getDescription() { return description; }

  public Integer getCurrentVersion() { return currentVersion; }

  public boolean isActive() { return active; }

  public Long getCreatedByUserId() { return createdByUserId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
