-- Flyway migration script: migrate entity_dynamic_mapping relations from map to array format
-- Purpose: The `relations` JSONB column previously stored relations as a flat JSON object
--          {"relationName": "jsltExpression"}.
--          It now stores them as an ordered JSON array
--          [{"name": "relationName", "expressions": ["jsltExpression"]}].
--          This migration converts all existing rows to the new array format.

-- Convert rows where relations is a non-empty JSON object (old map format)
-- e.g. {"owner": ".sender.login"} → [{"name": "owner", "expressions": [".sender.login"]}]
UPDATE entity_dynamic_mapping
SET relations = (
    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object('name', key, 'expressions', jsonb_build_array(value))
            ORDER BY key
        ),
        '[]'::jsonb
    )
    FROM jsonb_each_text(relations)
)
WHERE jsonb_typeof(relations) = 'object';

-- Normalize NULL relations to empty array (defensive guard for any unexpected NULLs)
UPDATE entity_dynamic_mapping
SET relations = '[]'::jsonb
WHERE relations IS NULL;

COMMENT ON COLUMN entity_dynamic_mapping.relations IS
    'JSLT relation mappings stored as a JSON array: [{"name":"<relation>","expressions":["<jslt>", ...]}]';
