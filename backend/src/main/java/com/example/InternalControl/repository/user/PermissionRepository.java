package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Permission entity.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByPermissionKey(String permissionKey);

    boolean existsByPermissionKey(String permissionKey);
}
