package com.example.InternalControl.repository;

import com.example.InternalControl.model.OrganizationDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationDocumentVersionRepository extends JpaRepository<OrganizationDocumentVersion, Long> {

  Optional<OrganizationDocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, Integer versionNumber);
}