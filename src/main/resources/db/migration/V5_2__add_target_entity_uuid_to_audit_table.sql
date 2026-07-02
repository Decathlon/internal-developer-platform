-- Flyway migration script: Add target_entity_uuid to audit table
-- Purpose: Adds the target_entity_uuid column to relation_target_entities_aud table
-- to keep the audit table in sync with V5.1 changes to the main table

ALTER TABLE relation_target_entities_aud
ADD COLUMN target_entity_uuid UUID;

-- Create index for better performance on audit queries
CREATE INDEX idx_relation_target_entities_aud_target_uuid
ON relation_target_entities_aud (target_entity_uuid);

-- Add table comment for documentation
COMMENT ON COLUMN relation_target_entities_aud.target_entity_uuid IS 'UUID identifier of the target entity; synced with V5.1 changes';
