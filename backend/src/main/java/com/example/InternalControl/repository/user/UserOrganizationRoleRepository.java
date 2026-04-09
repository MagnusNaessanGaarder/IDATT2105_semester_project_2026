package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserOrganizationRole entities.
 */
@Repository
public interface UserOrganizationRoleRepository extends JpaRepository<UserOrganizationRole, UserOrganizationRoleId> {

    @Query("SELECT uor FROM UserOrganizationRole uor " +
            "JOIN FETCH uor.role " +
            "WHERE uor.user.userId = :userId AND uor.organization.orgNumber = :orgNumber")
    List<UserOrganizationRole> findByUserOrganization(
            @Param("userId") Long userId,
            @Param("orgNumber") Integer orgNumber
    );

    @Query("SELECT uor FROM UserOrganizationRole uor " +
            "JOIN FETCH uor.role " +
            "WHERE uor.user.userId = :userId")
    List<UserOrganizationRole> findByUserId(@Param("userId") Long userId);

    @Query("SELECT uor FROM UserOrganizationRole uor " +
            "JOIN FETCH uor.role " +
            "WHERE uor.user.userId = :userId AND uor.organization.isActive = true")
    List<UserOrganizationRole> findRolesInActiveOrganizations(@Param("userId") Long userId);

    @Query("SELECT uor FROM UserOrganizationRole uor " +
            "JOIN FETCH uor.role " +
            "WHERE uor.user.userId = :userId AND uor.organization.orgNumber = :orgNumber")
    List<UserOrganizationRole> findByUserIdAndOrgNumber(
            @Param("userId") Long userId,
            @Param("orgNumber") Integer orgNumber
    );

    @Query("SELECT uor FROM UserOrganizationRole uor " +
             "WHERE uor.user.userId = :userId AND uor.organization.orgNumber = :orgNumber AND uor.role.roleId = :roleId")
    Optional<UserOrganizationRole> findByUserIdAndOrgNumberAndRoleId(
            @Param("userId") Long userId,
            @Param("orgNumber") Integer orgNumber,
            @Param("roleId") Long roleId
    );

    @Query("SELECT CASE WHEN COUNT(uor) > 0 THEN true ELSE false END FROM UserOrganizationRole uor " +
            "WHERE uor.id.userId = :userId AND uor.id.orgNumber = :orgNumber AND uor.id.roleId = :roleId")
    boolean existsByUserIdAndOrgNumberAndRoleId(
            @Param("userId") Long userId,
            @Param("orgNumber") Integer orgNumber,
            @Param("roleId") Long roleId
    );
}
