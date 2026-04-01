
package com.example.InternalControl.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class OrganizationSettings {
  @Id
  @Column(name = "org_number", nullable = false)
  private long orgNumber;

  @Column(name = "timezone_name", nullable = false)
  @Builder.Default
  private String timezoneName = "Europe/Oslo";

  @Column(name = "locale_code", nullable = false)
  @Builder.Default
  private String localCode = "nb-NO";

  @Column(name = "enable_food_module", nullable = false)
  @Builder.Default
  private boolean enableFoodModule = true;

  @Column(name = "enable_alcohol_module", nullable = false)
  @Builder.Default
  private boolean enableAlcoholModule = true;

  @Column(name = "default_temp_min_c", precision = 5, scale = 2)
  private BigDecimal defaultTempMinC;

  @Column(name = "default_temp_max_c", precision = 5, scale = 2)
  private BigDecimal defaultTempMaxC;

  @Column(name = "reminder_email_enabled", nullable = false)
  @Builder.Default
  private boolean reminderEmailEnabled = true;

  @Column(name = "notification_email")
  private String notificationEmail;

  @Column(name = "retention_user_months")
  private long retentionUserMonths;

  @Column(name = "retention_audit_months")
  private long retentionAuditMonths;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "org_number", referencedColumnName = "org_number", insertable = false, updatable = false)
  private Organization organization;
}
