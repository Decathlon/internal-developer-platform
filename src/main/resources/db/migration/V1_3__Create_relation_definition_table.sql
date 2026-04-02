-- Create relation_definition table
-- This table contains relationship definitions between entities

CREATE TABLE relation_definition (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    target_entity_identifier VARCHAR(255) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    to_many BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create indexes for better performance
CREATE INDEX idx_relation_definition_name ON relation_definition(name);
CREATE INDEX idx_relation_definition_target ON relation_definition(target_entity_identifier);

-- Add table comment
COMMENT ON TABLE relation_definition IS 'Relationship definitions between entities';

-- Add column comments
COMMENT ON COLUMN relation_definition.name IS 'Relation name identifier';
COMMENT ON COLUMN relation_definition.target_entity_identifier IS 'Identifier of the target entity type';
COMMENT ON COLUMN relation_definition.required IS 'Whether this relation is mandatory';
COMMENT ON COLUMN relation_definition.to_many IS 'Whether the relation allows multiple targets';
