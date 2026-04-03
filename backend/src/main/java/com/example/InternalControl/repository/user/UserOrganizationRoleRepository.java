package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.organization.UserOrganizationRole;
import com.example.InternalControl.model.organization.UserOrganizationRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserOrganizationRole entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface UserOrganizationRoleRepository extends JpaRepository<UserOrganizationRole, UserOrganizationRoleId> {

    /**
     * Find all roles for a specific user organization membership.
     */
    @Query("SELECT uor FROM UserOrganizationRole uor " +
           "WHERE uor.user.userId = :userId AND uor.organization.orgNumber = :orgNumber")
    List<UserOrganizationRole> findByUserOrganization(
            @Param("userId") Long userId,
            @Param("orgNumber") Integer orgNumber
    );

    /**
     * Find all roles for a user across all organizations.
     */
    @Query("SELECT uor FROM UserOrganizationRole uor WHERE uor.user.userId = :userId")
    List<UserOrganizationRole> findByUserId(@Param("userId") Long userId);

    /**
     * Find all roles for a user in active organizations only.
     */
    @Query("SELECT uor FROM UserOrganizationRole uor " +
           "WHERE uor.user.userId = :userId AND uor.organization.isActive = true")
    List<UserOrganizationRole> findRolesInActiveOrganizations(@Param("userId") Long userId);
}
