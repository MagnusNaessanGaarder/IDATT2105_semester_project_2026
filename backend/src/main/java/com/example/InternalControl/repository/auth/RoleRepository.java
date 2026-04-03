package com.example.InternalControl.repository.auth;

import com.example.InternalControl.model.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByRoleName(String roleName);
    
    boolean existsByRoleName(String roleName);
}
