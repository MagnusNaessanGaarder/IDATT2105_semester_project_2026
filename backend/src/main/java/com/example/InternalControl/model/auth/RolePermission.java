package com.example.InternalControl.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "role_permission")
@IdClass(RolePermissionId.class)
public class RolePermission {

  @Id
  @Column(name = "role_id")
  private Long roleId;

  @Id
  @Column(name = "permission_id")
  private Long permissionId;
}

@Getter
@Setter
class RolePermissionId implements Serializable {
  private Long roleId;
  private Long permissionId;
}
