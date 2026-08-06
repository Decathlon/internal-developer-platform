-- Flyway migration script: Remove unused modified flag columns from relation_target_entities_aud
-- Purpose: Hibernate Envers does not properly support withModifiedFlag for embeddables
-- within ElementCollections. These columns were never being populated correctly.
-- The collection-level modified flag (target_entities_mod in relation_aud) is sufficient
-- to track when the relation targets change.

ALTER TABLE relation_target_entities_aud
DROP COLUMN IF EXISTS relation_id_mod;

ALTER TABLE relation_target_entities_aud
DROP COLUMN IF EXISTS target_entity_identifier_mod;

ALTER TABLE relation_target_entities_aud
DROP COLUMN IF EXISTS target_entity_uuid_mod;

-- Add table comment for documentation
COMMENT ON TABLE relation_target_entities_aud IS 'Audit table for relation target entities. Modified flags removed as they are not supported for ElementCollection embeddables.';

ALTER TABLE entity_properties_aud
DROP COLUMN IF EXISTS entity_id_mod;

ALTER TABLE entity_properties_aud
DROP COLUMN IF EXISTS property_id_mod;

-- Add table comment for documentation
COMMENT ON TABLE entity_properties_aud IS 'Audit table for entity properties. Modified flags removed as they are not supported for ElementCollection embeddables.';

ALTER TABLE entity_relations_aud
DROP COLUMN IF EXISTS entity_id_mod;

ALTER TABLE entity_relations_aud
DROP COLUMN IF EXISTS relation_id_mod;

-- Add table comment for documentation
COMMENT ON TABLE entity_relations_aud IS 'Audit table for entity relations. Modified flags removed as they are not supported for ElementCollection embeddables.';

ALTER TABLE entity_template_properties_definitions_aud
DROP COLUMN IF EXISTS entity_template_id_mod;

ALTER TABLE entity_template_properties_definitions_aud
DROP COLUMN IF EXISTS properties_definitions_id_mod;

-- Add table comment for documentation
COMMENT ON TABLE entity_template_properties_definitions_aud IS 'Audit table for entity template properties definitions. Modified flags removed as they are not supported for ElementCollection embeddables.';


ALTER TABLE entity_template_relations_definitions_aud
DROP COLUMN IF EXISTS entity_template_id_mod;

ALTER TABLE entity_template_relations_definitions_aud
DROP COLUMN IF EXISTS relations_definitions_id_mod;

-- Add table comment for documentation
COMMENT ON TABLE entity_template_relations_definitions_aud IS 'Audit table for entity template relations definitions. Modified flags removed as they are not supported for ElementCollection embeddables.';
