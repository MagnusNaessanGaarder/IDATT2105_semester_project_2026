package com.example.InternalControl.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "organization_document_version")
public class OrganizationDocumentVersion {

  @Setter
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "document_version_id")
  private Long documentVersionId;

  @Setter
  @Column(name = "document_id", nullable = false)
  private Long documentId;

  @Setter
  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Setter
  @Column(name = "azure_container", nullable = false, length = 63)
  private String azureContainer;

  @Setter
  @Column(name = "azure_blob_name", nullable = false, length = 512)
  private String azureBlobName;

  @Setter
  @Column(name = "original_filename", nullable = false)
  private String originalFilename;

  @Setter
  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Setter
  @Column(name = "file_size_bytes", nullable = false)
  private Long fileSizeBytes;

  @Setter
  @Column(name = "blob_etag", length = 100)
  private String blobEtag;

  @Setter
  @Column(name = "uploaded_by_user_id")
  private Long uploadedByUserId;

  @Column(name = "uploaded_at", updatable = false)
  private LocalDateTime uploadedAt;

  @Setter
  @Column(name = "valid_from")
  private LocalDate validFrom;

  @Setter
  @Column(name = "valid_to")
  private LocalDate validTo;

  @Setter
  @Column(name = "checksum_sha256", length = 64)
  private String checksumSha256;

  @PrePersist
  protected void onCreate() {
    uploadedAt = LocalDateTime.now();
  }

}