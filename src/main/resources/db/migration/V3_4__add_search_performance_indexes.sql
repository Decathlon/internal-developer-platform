-- Flyway migration script: add search performance indexes
-- Purpose: Support the EXISTS-based search specifications and ILIKE-based operators
--          with index scans instead of full table scans.
--
-- Strategy:
--   - Btree functional lower(col) indexes → EQ / NEQ comparisons using LOWER()
--   - Btree indexes on relation columns     → exact equality lookups in EXISTS subqueries
--   - pg_trgm GIN indexes                  → ILIKE CONTAINS / ENDS_WITH / STARTS_WITH

-- Enable pg_trgm for GIN trigram indexes used by ILIKE operators
CREATE EXTENSION IF NOT EXISTS pg_trgm SCHEMA public;

-- ── Relation indexes ────────────────────────────────────────────────────────

-- Exact equality on relation name (used in all relation EXISTS subqueries)
CREATE INDEX idx_relation_name
    ON relation (name);

COMMENT ON INDEX idx_relation_name IS 'Supports exact relation name equality in EXISTS subqueries';

-- GIN trigram index for ILIKE-based relation name searches (CONTAINS, ENDS_WITH, STARTS_WITH)
CREATE INDEX idx_relation_name_trgm
    ON relation USING GIN (name gin_trgm_ops);

COMMENT ON INDEX idx_relation_name_trgm IS 'GIN trigram index for ILIKE pattern matching on relation name';

-- Reverse-relation lookup: target entity identifier in relationsAsTargetSpec
CREATE INDEX idx_relation_target_entities_identifier
    ON relation_target_entities (target_entity_identifier);

COMMENT ON INDEX idx_relation_target_entities_identifier IS 'Supports reverse relation lookups by target entity identifier';

-- ── Entity indexes ──────────────────────────────────────────────────────────

-- Functional btree indexes for EQ / NEQ which use LOWER(col) = lower_value
CREATE INDEX idx_entity_name_lower
    ON entity (lower(name));

CREATE INDEX idx_entity_identifier_lower
    ON entity (lower(identifier));

CREATE INDEX idx_entity_template_identifier_lower
    ON entity (lower(template_identifier));

COMMENT ON INDEX idx_entity_name_lower IS 'Supports LOWER(name) = value equality comparisons (EQ operator)';
COMMENT ON INDEX idx_entity_identifier_lower IS 'Supports LOWER(identifier) = value equality comparisons (EQ operator)';
COMMENT ON INDEX idx_entity_template_identifier_lower IS 'Supports LOWER(template_identifier) = value equality comparisons (EQ operator)';

-- GIN trigram indexes for ILIKE-based searches (CONTAINS, ENDS_WITH, STARTS_WITH)
CREATE INDEX idx_entity_name_trgm
    ON entity USING GIN (name gin_trgm_ops);

CREATE INDEX idx_entity_identifier_trgm
    ON entity USING GIN (identifier gin_trgm_ops);

CREATE INDEX idx_entity_template_identifier_trgm
    ON entity USING GIN (template_identifier gin_trgm_ops);

COMMENT ON INDEX idx_entity_name_trgm IS 'GIN trigram index for ILIKE pattern matching on entity name';
COMMENT ON INDEX idx_entity_identifier_trgm IS 'GIN trigram index for ILIKE pattern matching on entity identifier';
COMMENT ON INDEX idx_entity_template_identifier_trgm IS 'GIN trigram index for ILIKE pattern matching on entity template identifier';

-- ── Property indexes ────────────────────────────────────────────────────────

-- GIN trigram index for ILIKE-based property value searches
CREATE INDEX idx_property_value_trgm
    ON property USING GIN (value gin_trgm_ops);

COMMENT ON INDEX idx_property_value_trgm IS 'GIN trigram index for ILIKE pattern matching on property value';
