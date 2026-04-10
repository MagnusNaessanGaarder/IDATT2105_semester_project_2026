-- Add org profile fields (name, email, phone) to settings
ALTER TABLE organization_settings
    ADD COLUMN display_name VARCHAR(255) NULL AFTER notification_email,
    ADD COLUMN legal_name VARCHAR(255) NULL AFTER display_name,
    ADD COLUMN contact_email VARCHAR(255) NULL AFTER legal_name,
    ADD COLUMN contact_phone VARCHAR(50) NULL AFTER contact_email;

-- Add comments for documentation
ALTER TABLE organization_settings
    MODIFY COLUMN display_name VARCHAR(255) NULL COMMENT 'Display name for the organization in the application',
    MODIFY COLUMN legal_name VARCHAR(255) NULL COMMENT 'Official registered legal name of the organization',
    MODIFY COLUMN contact_email VARCHAR(255) NULL COMMENT 'Primary contact email address for the organization',
    MODIFY COLUMN contact_phone VARCHAR(50) NULL COMMENT 'Contact phone number for the organization';
