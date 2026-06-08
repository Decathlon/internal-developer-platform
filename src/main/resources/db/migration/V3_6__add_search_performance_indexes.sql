-- Flyway migration script: add search performance indexes
-- Purpose: Accelerate the search endpoint with GIN trigram indexes (ILIKE pattern matching)
--          and functional btree indexes (EQ/NEQ equality matching).
--
-- Strategy:
--   - GIN trigram indexes (public.gin_trgm_ops) on raw columns → ILIKE with CONTAINS, ENDS_WITH,
--     STARTS_WITH, NOT_CONTAINS operators and globalTextSearch. Operator class is schema-qualified
--     because the application connection uses search_path = idp_core which does not include public.
--   - Functional btree lower(col) indexes → EQ / NEQ comparisons using LOWER(col)
--   - Btree indexes on relation columns → exact equality lookups in EXISTS subqueries
--   - The pg_trgm extension is managed by infrastructure — no CREATE EXTENSION here.

-- =========================================================================
-- Relation Indexes
-- =========================================================================

-- Exact equality on relation name (used in all relation EXISTS subqueries)
CREATE INDEX idx_relation_name
    ON relation (name);

COMMENT ON INDEX idx_relation_name IS 'Supports exact relation name equality in EXISTS subqueries';

-- Reverse-relation lookup: target entity identifier in relationsAsTargetSpec
CREATE INDEX idx_relation_target_entities_identifier
    ON relation_target_entities (target_entity_identifier);

COMMENT ON INDEX idx_relation_target_entities_identifier IS 'Supports reverse relation lookups by target entity identifier';

-- GIN trigram index for ILIKE-based relation name searches (CONTAINS, STARTS_WITH, ENDS_WITH)
CREATE INDEX idx_relation_name_trgm
    ON relation USING GIN (name public.gin_trgm_ops);

COMMENT ON INDEX idx_relation_name_trgm IS 'GIN trigram index for ILIKE pattern matching on relation name';

-- =========================================================================
-- Entity Indexes
-- =========================================================================

-- Functional btree indexes for EQ / NEQ which use LOWER(col)
CREATE INDEX idx_entity_name_lower
    ON entity (lower(name));

CREATE INDEX idx_entity_identifier_lower
    ON entity (lower(identifier));

CREATE INDEX idx_entity_template_identifier_lower
    ON entity (lower(template_identifier));

COMMENT ON INDEX idx_entity_name_lower IS 'Supports LOWER(name) comparisons for EQ and NEQ operators';
COMMENT ON INDEX idx_entity_identifier_lower IS 'Supports LOWER(identifier) comparisons for EQ and NEQ operators';
COMMENT ON INDEX idx_entity_template_identifier_lower IS 'Supports LOWER(template_identifier) comparisons for EQ and NEQ operators';

-- GIN trigram indexes for ILIKE-based entity field searches
CREATE INDEX idx_entity_name_trgm
    ON entity USING GIN (name public.gin_trgm_ops);

CREATE INDEX idx_entity_identifier_trgm
    ON entity USING GIN (identifier public.gin_trgm_ops);

CREATE INDEX idx_entity_template_identifier_trgm
    ON entity USING GIN (template_identifier public.gin_trgm_ops);

COMMENT ON INDEX idx_entity_name_trgm IS 'GIN trigram index for ILIKE pattern matching on entity name';
COMMENT ON INDEX idx_entity_identifier_trgm IS 'GIN trigram index for ILIKE pattern matching on entity identifier';
COMMENT ON INDEX idx_entity_template_identifier_trgm IS 'GIN trigram index for ILIKE pattern matching on entity template identifier';

-- =========================================================================
-- Property Indexes
-- =========================================================================

-- GIN trigram index for ILIKE-based property value searches
CREATE INDEX idx_property_value_trgm
    ON property USING GIN (value public.gin_trgm_ops);

COMMENT ON INDEX idx_property_value_trgm IS 'GIN trigram index for ILIKE pattern matching on property value';
