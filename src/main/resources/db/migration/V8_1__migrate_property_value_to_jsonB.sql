-- Flyway migration script: Safely migrate value type to jsonB
-- Purpose: able to manage string list, and json compatible type on property.value without breaking data

-- 1. Create a new column with the JSONB type
ALTER TABLE idp_core.property
    ADD COLUMN value_jsonb JSONB;

ALTER TABLE idp_core.property_aud
    ADD COLUMN value_jsonb JSONB;

-- 2. Migrate existing text data to the new JSONB column
-- The to_jsonb function safely wraps the existing text into a valid JSON string (e.g., 'test' becomes '"test"')
UPDATE idp_core.property
SET value_jsonb = to_jsonb(value);

UPDATE idp_core.property_aud
SET value_jsonb = CASE
                      WHEN value IS NULL THEN NULL
                      ELSE to_jsonb(value)
    END;

-- 3. Drop the old trigger/index on the old column
DROP INDEX IF EXISTS idp_core.idx_property_value_trgm;

-- 4. Drop the old text column
ALTER TABLE idp_core.property
DROP COLUMN value;

ALTER TABLE idp_core.property_aud
DROP COLUMN value;

-- 5. Rename the new JSONB column to the original name
ALTER TABLE idp_core.property
    RENAME COLUMN value_jsonb TO value;

ALTER TABLE idp_core.property_aud
    RENAME COLUMN value_jsonb TO value;

-- 6. Create the new GIN index optimized for JSONB on the new column
CREATE INDEX idx_property_value_gin
    ON idp_core.property
    USING GIN (value);
