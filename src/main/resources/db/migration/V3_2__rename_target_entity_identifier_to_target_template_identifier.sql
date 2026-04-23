-- Rename target_entity_identifier to target_template_identifier

-- Drop the old index
DROP INDEX idx_relation_definition_target;

-- Rename column in relation_definition table
ALTER TABLE relation_definition
    RENAME COLUMN target_entity_identifier TO target_template_identifier;

-- Create new index with updated name
CREATE INDEX idx_relation_definition_target ON relation_definition(target_template_identifier);

COMMENT ON COLUMN relation_definition.target_template_identifier IS 'Identifier of the target entity template';
