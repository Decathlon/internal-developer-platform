-- Flyway migration script: rename "expressions" to "targetIdentifiersExpressions" in entity_dynamic_mapping relations
-- Purpose: V6_4 migrated the relations column from a flat JSON object
--          {"relationName": "jsltExpression"} to a JSON array
--          [{"name": "relationName", "expressions": ["jsltExpression"]}].
--          The domain model now uses the field name "targetIdentifiersExpressions".
--          This migration renames the key in every array element that still carries
--          the old "expressions" key.

-- Rename "expressions" → "targetIdentifiersExpressions" in each array element
-- Only rows that are arrays and still contain at least one element with the old key are updated.
UPDATE entity_dynamic_mapping
SET relations = (
    SELECT jsonb_agg(
        CASE
            WHEN elem ? 'expressions' AND NOT elem ? 'targetIdentifiersExpressions'
                THEN (elem - 'expressions')
                    || jsonb_build_object('targetIdentifiersExpressions', elem -> 'expressions')
            ELSE elem
        END
        ORDER BY ordinality
    )
    FROM jsonb_array_elements(relations) WITH ORDINALITY AS t(elem, ordinality)
)
WHERE jsonb_typeof(relations) = 'array'
  AND EXISTS (
      SELECT 1
      FROM jsonb_array_elements(relations) AS elem
      WHERE elem ? 'expressions'
        AND NOT elem ? 'targetIdentifiersExpressions'
  );

-- Normalize NULL relations (SQL NULL or JSONB null) to empty array (defensive guard)
UPDATE entity_dynamic_mapping
SET relations = '[]'::jsonb
WHERE relations IS NULL OR relations = 'null'::jsonb;

COMMENT ON COLUMN entity_dynamic_mapping.relations IS
    'JSLT relation mappings stored as a JSON array: [{"name":"<relation>","targetIdentifiersExpressions":["<jslt>", ...]}]';
