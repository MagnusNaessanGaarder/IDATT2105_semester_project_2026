package com.example.InternalControl.repository.organization;

import com.example.InternalControl.model.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Organization entity.
 * Primary key is org_number (Integer).
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
}
