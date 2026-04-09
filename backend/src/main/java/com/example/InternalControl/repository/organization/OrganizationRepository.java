package com.example.InternalControl.repository.organization;

import com.example.InternalControl.model.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Organization} entities.
 * Primary key is organization number (Integer).
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
}
