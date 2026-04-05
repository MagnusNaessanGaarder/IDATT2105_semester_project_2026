package com.example.InternalControl.model.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping to app_user_local_credential table.
 * Separate table for security reasons.
 */
@Entity
@Table(name = "app_user_local_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserLocalCredential {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "credential_id")
  private Long credentialId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private AppUser user;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "must_change_pw", nullable = false)
  private Boolean mustChangePw = false;

  @Column(name = "last_changed_at", nullable = false)
  private LocalDateTime lastChangedAt;

  @Column(name = "failed_attempts", nullable = false)
  private Integer failedAttempts = 0;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public boolean isLocked() {
    return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
  }

  public void recordFailedAttempt() {
    this.failedAttempts++;
    if (this.failedAttempts >= 5) {
      this.lockedUntil = LocalDateTime.now().plusMinutes(30);
    }
  }

  public void resetFailedAttempts() {
    this.failedAttempts = 0;
    this.lockedUntil = null;
  }
}
