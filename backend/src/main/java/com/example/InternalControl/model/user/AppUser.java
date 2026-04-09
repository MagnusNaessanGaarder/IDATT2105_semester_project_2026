package com.example.InternalControl.model.user;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA Entity representing a user in the system.
 * Maps to the app_user table. Passwords are stored separately in {@link AppUserLocalCredential}.
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
  @JsonIgnore
  private AppUserLocalCredential localCredential;

  public boolean isLocked() {
    return localCredential != null && localCredential.isLocked();
  }

  public boolean isActive() {
    return isActive != null && isActive;
  }
}
