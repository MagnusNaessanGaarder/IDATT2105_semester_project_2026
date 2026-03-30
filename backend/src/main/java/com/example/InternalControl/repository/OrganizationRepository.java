package com.example.InternalControl.repository;

import com.example.InternalControl.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Organization entity.
 * Primary key is org_number (Integer).
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
}
