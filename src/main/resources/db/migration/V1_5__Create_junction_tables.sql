-- Create junction tables for entity relationships
-- These tables handle many-to-many relationships between entity templates and their definitions

-- Junction table for entity_template -> property_definition (OneToMany)
CREATE TABLE entity_template_properties_definitions (
    entity_template_id UUID NOT NULL,
    properties_definitions_id UUID NOT NULL,
    CONSTRAINT pk_entity_template_properties PRIMARY KEY (entity_template_id, properties_definitions_id),
    CONSTRAINT fk_entity_template_properties_template FOREIGN KEY (entity_template_id) REFERENCES entity_template(id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_template_properties_definition FOREIGN KEY (properties_definitions_id) REFERENCES property_definition(id) ON DELETE CASCADE
);

-- Junction table for entity_template -> relation_definition (OneToMany)
CREATE TABLE entity_template_relations_definitions (
    entity_template_id UUID NOT NULL,
    relations_definitions_id UUID NOT NULL,
    CONSTRAINT pk_entity_template_relations PRIMARY KEY (entity_template_id, relations_definitions_id),
    CONSTRAINT fk_entity_template_relations_template FOREIGN KEY (entity_template_id) REFERENCES entity_template(id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_template_relations_definition FOREIGN KEY (relations_definitions_id) REFERENCES relation_definition(id) ON DELETE CASCADE
);

-- Add table comments
COMMENT ON TABLE entity_template_properties_definitions IS 'Junction table linking templates to property definitions';
COMMENT ON TABLE entity_template_relations_definitions IS 'Junction table linking templates to relation definitions';

-- Add column comments
COMMENT ON COLUMN entity_template_properties_definitions.entity_template_id IS 'Reference to entity template';
COMMENT ON COLUMN entity_template_properties_definitions.properties_definitions_id IS 'Reference to property definition';
COMMENT ON COLUMN entity_template_relations_definitions.entity_template_id IS 'Reference to entity template';
COMMENT ON COLUMN entity_template_relations_definitions.relations_definitions_id IS 'Reference to relation definition';
