-- Flyway migration script: Add updated_at to entity and entity_aud tables
-- Purpose: Adds the updated_at column to entity table
-- and updates the audit table with modification flags to keep Envers happy

-- entity table
ALTER TABLE entity
    ADD COLUMN updated_at BIGINT;

-- entity audit table
ALTER TABLE entity_aud
    ADD COLUMN updated_at BIGINT,
    ADD COLUMN updated_at_mod BOOLEAN;
