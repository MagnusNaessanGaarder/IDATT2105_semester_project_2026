package com.example.InternalControl.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "permission")
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "permission_id")
  private Long permissionId;

  @Column(name = "permission_key", nullable = false, unique = true, length = 100)
  private String permissionKey;

  @Column(name = "description", length = 255)
  private String description;
}
