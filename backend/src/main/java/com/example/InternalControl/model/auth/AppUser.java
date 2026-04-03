package com.example.InternalControl.model.auth;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA Entity mapping to app_user table
 * No passwords
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name="app_user")
@AllArgsConstructor
@Getter
@Setter
@Builder
@NoArgsConstructor
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "email", unique = true)
  private String email;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "phone")
  private String phone;

  @Column(name = "global_last_seen_at")
  private LocalDateTime globalLastSeenAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private AppUserLocalCredential localCredential;

  public boolean isLocked() {
    return localCredential != null && localCredential.isLocked();
  }

  public boolean isActive() {
    return isActive != null && isActive;
  }
}
