-- Change property.value to TEXT, previous VARCHAR(255) is too short for properties like description
-- Remove constraint UNIQUE (template_identifier, name) that prevented multiple entities from sharing the same display name within a template

ALTER TABLE property
    ALTER COLUMN value TYPE TEXT;

ALTER TABLE entity
    DROP CONSTRAINT entity_template_identifier_name_key;
