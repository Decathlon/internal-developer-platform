-- This migration creates the 'entity_dynamic_mapping' table to store dynamic mappings of entities based on templates and filters.

CREATE TABLE entity_dynamic_mapping
(
    id                  UUID PRIMARY KEY,
    identifier VARCHAR(255) UNIQUE NOT NULL,
    template_identifier VARCHAR(255) NOT NULL,
    filter              VARCHAR(255) NOT NULL,
    entity_identifier   VARCHAR(255) NOT NULL,
    entity_title        VARCHAR(255) NOT NULL,
    properties          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    relations           JSONB        NOT NULL DEFAULT '{}'::jsonb
);
