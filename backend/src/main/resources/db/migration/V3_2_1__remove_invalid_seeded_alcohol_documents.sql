-- Remove seeded alcohol documents that are neither actual law/forskrift references
-- nor backed by a real blob upload. These should not appear in the regulations list.

DELETE odv
FROM organization_document_version odv
JOIN organization_document od ON od.document_id = odv.document_id
WHERE od.org_number = 937219997
  AND od.title IN (
      'Eksempel: daglig alderskontroll ved servering',
      'Eksempel: handtering av berusede gjester',
      'Eksempelhefte for ansvarlig alkoholservering'
  );

DELETE FROM organization_document
WHERE org_number = 937219997
  AND title IN (
      'Eksempel: daglig alderskontroll ved servering',
      'Eksempel: handtering av berusede gjester',
      'Eksempelhefte for ansvarlig alkoholservering'
  );
