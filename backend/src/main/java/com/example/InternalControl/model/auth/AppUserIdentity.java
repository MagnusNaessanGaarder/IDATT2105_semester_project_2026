package com.example.InternalControl.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "app_user_identity")
public class AppUserIdentity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "identity_id")
  private Long identityId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "provider_name", nullable = false, length = 50)
  private String providerName;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @Column(name = "provider_email", length = 255)
  private String providerEmail;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
