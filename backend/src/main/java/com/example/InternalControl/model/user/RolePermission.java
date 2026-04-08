package com.example.InternalControl.model.user;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity mapping to role_permission join table.
 * Represents the many-to-many relationship between roles and permissions.
 */
@Entity
@Table(name = "role_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(RolePermissionId.class)
public class RolePermission {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Id
    @Column(name = "permission_id")
    private Long permissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", insertable = false, updatable = false)
    private Permission permission;
}
