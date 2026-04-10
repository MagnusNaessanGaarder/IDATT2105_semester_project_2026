package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AppUser entities.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.localCredential WHERE u.email = :email")
    Optional<AppUser> findByEmailWithCredentials(@Param("email") String email);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.localCredential WHERE u.userId = :userId")
    Optional<AppUser> findByIdWithCredentials(@Param("userId") Long userId);

    @Query(value = "SELECT r.role_name FROM app_user u " +
           "JOIN user_organization_role uor ON u.user_id = uor.user_id " +
           "JOIN role r ON uor.role_id = r.role_id " +
           "WHERE u.email = :email", nativeQuery = true)
    Optional<String> findRoleByEmail(@Param("email") String email);

    @Query(value = """
            SELECT DISTINCT u.*
            FROM app_user u
            JOIN user_organization uo ON u.user_id = uo.user_id
            JOIN user_organization_role uor ON u.user_id = uor.user_id AND uo.org_number = uor.org_number
            JOIN role r ON uor.role_id = r.role_id
            WHERE uo.org_number = :orgNumber
              AND uo.is_active = true
              AND u.is_active = true
              AND r.role_name IN (:roleNames)
            """, nativeQuery = true)
    List<AppUser> findActiveByOrgAndRoles(@Param("orgNumber") Integer orgNumber,
                                           @Param("roleNames") Collection<String> roleNames);
}
