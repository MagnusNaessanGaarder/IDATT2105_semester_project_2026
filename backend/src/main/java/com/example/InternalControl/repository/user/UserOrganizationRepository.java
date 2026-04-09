package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.model.user.UserOrganizationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserOrganization entities.
 */
@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UserOrganizationId> {

  @Query("SELECT uo FROM UserOrganization uo " +
      "WHERE uo.user.userId = :userId AND uo.isActive = true")
  List<UserOrganization> findActiveOrganizationsByUserId(@Param("userId") Long userId);

  @Query("SELECT uo FROM UserOrganization uo WHERE uo.user.userId = :userId")
  List<UserOrganization> findByUserId(@Param("userId") Long userId);

  @Query("SELECT uo FROM UserOrganization uo " +
      "WHERE uo.user.userId = :userId AND uo.organization.orgNumber = :orgNumber")
  Optional<UserOrganization> findByUserIdAndOrgNumber(
      @Param("userId") Long userId,
      @Param("orgNumber") Integer orgNumber);

  @Query("SELECT COUNT(uo) > 0 FROM UserOrganization uo " +
      "WHERE uo.user.userId = :userId AND uo.organization.orgNumber = :orgNumber")
  boolean existsByUserIdAndOrgNumber(
      @Param("userId") Long userId,
      @Param("orgNumber") Integer orgNumber);

    @Query("SELECT uo FROM UserOrganization uo JOIN FETCH uo.user WHERE uo.organization.orgNumber = :orgNumber")
    List<UserOrganization> findByOrgNumber(@Param("orgNumber") Integer orgNumber);
}
