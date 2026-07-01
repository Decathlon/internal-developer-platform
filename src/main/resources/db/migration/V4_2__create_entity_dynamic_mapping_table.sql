-- This migration creates the 'entity_dynamic_mapping' table to store dynamic mappings of entities based on templates and filters.

CREATE TABLE entity_dynamic_mapping
(
    id                  UUID PRIMARY KEY,
    identifier          VARCHAR(255) UNIQUE NOT NULL,
    template_identifier VARCHAR(255)        NOT NULL,
    filter              TEXT                NOT NULL,
    name                VARCHAR(255)        NOT NULL,
    description         TEXT,
    entity_identifier   TEXT                NOT NULL,
    entity_title        TEXT                NOT NULL,
    properties          JSONB               NOT NULL DEFAULT '{}'::jsonb,
    relations           JSONB               NOT NULL DEFAULT '{}'::jsonb
);
