
package com.example.InternalControl.repository;

import com.example.InternalControl.model.UserOrganization;
import com.example.InternalControl.model.UserOrganizationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserOrganization entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UserOrganizationId> {

  /**
   * Find all active organizations for a user.
   */
  @Query("SELECT uo FROM UserOrganization uo " +
      "WHERE uo.user.userId = :userId AND uo.isActive = true")
  List<UserOrganization> findActiveOrganizationsByUserId(@Param("userId") Long userId);

  /**
   * Find all organizations for a user (including inactive).
   */
  @Query("SELECT uo FROM UserOrganization uo WHERE uo.user.userId = :userId")
  List<UserOrganization> findByUserId(@Param("userId") Long userId);

  /**
   * Find specific organization membership for a user.
   */
  @Query("SELECT uo FROM UserOrganization uo " +
      "WHERE uo.user.userId = :userId AND uo.organization.orgNumber = :orgNumber")
  Optional<UserOrganization> findByUserIdAndOrgNumber(
      @Param("userId") Long userId,
      @Param("orgNumber") Integer orgNumber);

  /**
   * Check if user is a member of an organization.
   */
  @Query("SELECT COUNT(uo) > 0 FROM UserOrganization uo " +
      "WHERE uo.user.userId = :userId AND uo.organization.orgNumber = :orgNumber")
  boolean existsByUserIdAndOrgNumber(
      @Param("userId") Long userId,
      @Param("orgNumber") Integer orgNumber);
}
