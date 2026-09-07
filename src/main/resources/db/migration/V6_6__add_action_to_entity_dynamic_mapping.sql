-- Flyway migration script: add action column to entity_dynamic_mapping
-- Purpose: Add the action field to store the action type for dynamic entity mappings

-- Add action column as VARCHAR to store the enum name directly
ALTER TABLE entity_dynamic_mapping
    ADD COLUMN action VARCHAR(50) NOT NULL DEFAULT 'UPDATE_ENTITY';

-- Add column comment
COMMENT ON COLUMN entity_dynamic_mapping.action IS 'Action type for the entity dynamic mapping: UPDATE_ENTITY, UPDATE_PROPERTIES, UPDATE_RELATIONS, DELETE_ENTITY';
