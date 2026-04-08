package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.Permission;
import com.example.InternalControl.model.user.RolePermission;
import com.example.InternalControl.model.user.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for RolePermission entity.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.roleId = :roleId")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
