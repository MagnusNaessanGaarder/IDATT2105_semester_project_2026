package com.example.InternalControl.repository;

import com.example.InternalControl.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AppUser entities.
 *
 * @author TriTacLe
 * @since 1.0
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
}
