-- Flyway migration script to create Entity table

CREATE TABLE entity (
    id UUID PRIMARY KEY,
    template_identifier VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    identifier VARCHAR(255) UNIQUE,
    CONSTRAINT entity_template_identifier_name_key UNIQUE (template_identifier, name)
);
