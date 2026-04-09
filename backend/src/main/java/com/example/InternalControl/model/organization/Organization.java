package com.example.InternalControl.model.organization;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing an organization (tenant) in the system.
 * Maps to the organization table. Each organization has a unique orgNumber.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "organization")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

  @Id
  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Column(name = "legal_name", nullable = false)
  private String legalName;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "contact_email")
  private String contactEmail;

  @Column(name = "contact_phone")
  private String contactPhone;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
