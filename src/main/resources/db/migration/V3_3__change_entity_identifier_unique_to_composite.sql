-- Change unique constraint on entity table:
-- Drop the unique constraint on identifier alone
-- Add a composite unique constraint on (identifier, template_identifier)
-- This allows the same identifier to exist across different templates

ALTER TABLE entity DROP CONSTRAINT entity_identifier_key;

ALTER TABLE entity ADD CONSTRAINT entity_identifier_template_identifier_key
    UNIQUE (identifier, template_identifier);
