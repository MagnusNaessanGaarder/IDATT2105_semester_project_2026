package com.example.InternalControl.model.user;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA Entity mapping to app_user table.
 * No passwords stored here.
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

  @Column(name = "is_sysadmin", nullable = false)
  @Builder.Default
  private Boolean isSysadmin = false;

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
  @JsonIgnore
  private AppUserLocalCredential localCredential;

  public boolean isLocked() {
    return localCredential != null && localCredential.isLocked();
  }

  public boolean isActive() {
    return isActive != null && isActive;
  }
}