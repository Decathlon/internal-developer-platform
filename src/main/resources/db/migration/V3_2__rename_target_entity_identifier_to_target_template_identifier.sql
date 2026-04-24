-- Rename target_entity_identifier to target_template_identifier

-- Rename column in relation_definition table
ALTER TABLE relation_definition
    RENAME COLUMN target_entity_identifier TO target_template_identifier;

-- Rename index for consistency
ALTER INDEX idx_relation_definition_target RENAME TO idx_relation_definition_target_template;

COMMENT ON COLUMN relation_definition.target_template_identifier IS 'Identifier of the target entity template';
