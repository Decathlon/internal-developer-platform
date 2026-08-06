-- Flyway migration script: add action column to entity_dynamic_mapping
-- Purpose: Add the action field to store the action type for dynamic entity mappings

-- Add action column with a default value to handle existing rows
ALTER TABLE entity_dynamic_mapping
ADD COLUMN action SMALLINT NOT NULL DEFAULT 0;

-- Add column comment
COMMENT ON COLUMN entity_dynamic_mapping.action IS 'Action type for the entity dynamic mapping (UPSERT=0, UPSERT_PROPERTIES=1, UPSERT_RELATIONS=2, DELETE=3)';
