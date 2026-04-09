-- Seed alcohol regulation documents and example material for Everest Sushi

-- ---------------------------------------------------------
-- ALCOHOL REGULATIONS AND EXAMPLES
-- ---------------------------------------------------------

INSERT INTO organization_document (
    org_number,
    document_type,
    title,
    description,
    current_version,
    is_active,
    created_by_user_id,
    created_at,
    updated_at
)
SELECT 937219997,
       seed.document_type,
       seed.title,
       seed.description,
       1,
       1,
       (SELECT user_id FROM app_user WHERE email = seed.created_by_email LIMIT 1),
       NOW(),
       NOW()
FROM (
    SELECT 'POLICY' AS document_type,
           'Alkoholloven - sentrale krav og aldersgrenser' AS title,
           'Oppsummering av sentrale krav i alkoholloven, inkludert alderskontroll, beruselse og skjenketider.' AS description,
           'manager@everest-sushi.no' AS created_by_email
    UNION ALL
    SELECT 'POLICY',
           'Alkoholforskriften - lokale rutiner og internkontroll',
           'Praktisk veiledning til alkoholforskriften med fokus pa internkontroll, bevilling og ansvarlig drift.',
           'manager@everest-sushi.no'
    UNION ALL
    SELECT 'POLICY',
           'Serveringsloven - bevilling og ansvar',
           'Internt styringsdokument for krav til serveringsbevilling, styrer og stedfortreder.',
           'admin@everest-sushi.no'
    UNION ALL
    SELECT 'PROCEDURE',
           'Eksempel: daglig alderskontroll ved servering',
           'Eksempeldokument som viser hvordan ansatte skal gjennomfore og dokumentere alderskontroll i praksis.',
           'admin@everest-sushi.no'
    UNION ALL
    SELECT 'PROCEDURE',
           'Eksempel: handtering av berusede gjester',
           'Scenario-basert eksempel pa trygg handtering, avvisning og avviksregistrering ved tegn pa beruselse.',
           'manager@everest-sushi.no'
    UNION ALL
    SELECT 'TRAINING_MATERIAL',
           'Eksempelhefte for ansvarlig alkoholservering',
           'Kort opplaeringshefte med eksempler pa legitimasjonskontroll, skjenkestopp og intern rapportering.',
           'admin@everest-sushi.no'
) AS seed
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_document d
    WHERE d.org_number = 937219997
      AND d.title = seed.title
);

INSERT INTO organization_document_version (
    document_id,
    version_number,
    azure_container,
    azure_blob_name,
    original_filename,
    mime_type,
    file_size_bytes,
    blob_etag,
    uploaded_by_user_id,
    uploaded_at,
    valid_from,
    valid_to,
    checksum_sha256
)
SELECT d.document_id,
       1,
       'documents',
       CONCAT('org-937219997/alcohol/', REPLACE(REPLACE(LOWER(d.title), ' ', '-'), ':', ''), '.pdf'),
       CONCAT(d.title, '.pdf'),
       'application/pdf',
       CASE d.document_type
           WHEN 'TRAINING_MATERIAL' THEN 184320
           WHEN 'PROCEDURE' THEN 143360
           ELSE 122880
       END,
       NULL,
       d.created_by_user_id,
       NOW(),
       '2026-04-09',
       NULL,
       NULL
FROM organization_document d
WHERE d.org_number = 937219997
  AND d.title IN (
      'Alkoholloven - sentrale krav og aldersgrenser',
      'Alkoholforskriften - lokale rutiner og internkontroll',
      'Serveringsloven - bevilling og ansvar',
      'Eksempel: daglig alderskontroll ved servering',
      'Eksempel: handtering av berusede gjester',
      'Eksempelhefte for ansvarlig alkoholservering'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM organization_document_version v
      WHERE v.document_id = d.document_id
        AND v.version_number = 1
  );
