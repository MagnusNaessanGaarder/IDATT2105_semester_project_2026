package com.example.InternalControl.repository.document;

import com.example.InternalControl.model.document.OrganizationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationDocumentRepository extends JpaRepository<OrganizationDocument, Long> {

  Optional<OrganizationDocument> findByDocumentIdAndOrgNumber(Long documentId, Integer orgNumber);

  List<OrganizationDocument> findByOrgNumberAndActiveTrue(Integer orgNumber);

  List<OrganizationDocument> findByOrgNumberAndDocumentType(Integer orgNumber, String documentType);
}
