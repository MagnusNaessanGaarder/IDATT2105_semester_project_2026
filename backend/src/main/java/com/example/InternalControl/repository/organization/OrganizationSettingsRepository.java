package com.example.InternalControl.repository.organization;

import com.example.InternalControl.model.organization.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Long> {
}
