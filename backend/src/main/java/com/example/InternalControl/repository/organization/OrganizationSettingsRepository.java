package com.example.InternalControl.repository.organization;

import com.example.InternalControl.model.organization.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for OrganizationSettings entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Integer> {
}