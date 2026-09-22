-- Flyway migration script: Safely migrate property values to JSONB
-- Purpose: preserve legacy scalar text while enabling native JSON-compatible values.

DROP INDEX IF EXISTS idx_property_value_trgm;

ALTER TABLE property
ALTER COLUMN value TYPE JSONB USING to_jsonb(value),
    ALTER COLUMN value SET NOT NULL;

ALTER TABLE property_aud
ALTER COLUMN value TYPE JSONB USING to_jsonb(value);

-- 6. Create the new GIN index optimized for JSONB on the new column
CREATE INDEX idx_property_value_gin
    ON property
    USING GIN (value);

CREATE OR REPLACE FUNCTION idp_jsonb_root_text(value JSONB)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
STRICT
AS $$
SELECT value #>> '{}'
$$;

CREATE INDEX idx_property_value_trgm
    ON property
    USING GIN (idp_jsonb_root_text(value) public.gin_trgm_ops);
