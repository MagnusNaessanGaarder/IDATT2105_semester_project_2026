package com.example.InternalControl.model.user;

import com.example.InternalControl.model.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to user_organization table.
 * Represents the membership of a user in an organization.
 */
@Entity
@Table(name = "user_organization")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrganization {

  @EmbeddedId
  private UserOrganizationId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("orgNumber")
  @JoinColumn(name = "org_number", nullable = false)
  private Organization organization;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "joined_at", nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  @Column(name = "left_at")
  private LocalDateTime leftAt;

  @Column(name = "last_seen_at")
  private LocalDateTime lastSeenAt;

  // @UpdateTimestamp - REMOVED: updated_at column doesnt exist in database, maybe
  // it should @Anine
  // @Column(name = "updated_at", nullable = false)
  // private LocalDateTime updatedAt;

  public UserOrganizationId getId() {
    return id;
  }

  public void setId(UserOrganizationId id) {
    this.id = id;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean active) {
    isActive = active;
  }

  public LocalDateTime getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(LocalDateTime joinedAt) {
    this.joinedAt = joinedAt;
  }

  public LocalDateTime getLeftAt() {
    return leftAt;
  }

  public void setLeftAt(LocalDateTime leftAt) {
    this.leftAt = leftAt;
  }

  public LocalDateTime getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(LocalDateTime lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }
}
