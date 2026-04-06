package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserOrganizationRole entities.
 */
@Repository
public interface UserOrganizationRoleRepository extends JpaRepository<UserOrganizationRole, UserOrganizationRoleId> {

    @Query("SELECT uor FROM UserOrganizationRole uor " +
           "WHERE uor.user.userId = :userId AND uor.organization.orgNumber = :orgNumber")
    List<UserOrganizationRole> findByUserOrganization(
            @Param("userId") Long userId,
            @Param("orgNumber") Integer orgNumber
    );

    @Query("SELECT uor FROM UserOrganizationRole uor WHERE uor.user.userId = :userId")
    List<UserOrganizationRole> findByUserId(@Param("userId") Long userId);

    @Query("SELECT uor FROM UserOrganizationRole uor " +
           "WHERE uor.user.userId = :userId AND uor.organization.isActive = true")
    List<UserOrganizationRole> findRolesInActiveOrganizations(@Param("userId") Long userId);
}
