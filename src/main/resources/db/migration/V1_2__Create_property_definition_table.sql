-- Create property_definition table
-- This table contains property definitions with validation rules

CREATE TABLE property_definition (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('STRING', 'NUMBER', 'BOOLEAN')),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    rules_id UUID,
    CONSTRAINT fk_property_definition_rules FOREIGN KEY (rules_id) REFERENCES property_rules(id)
);

-- Create index for better performance
CREATE INDEX idx_property_definition_name ON property_definition(name);
CREATE INDEX idx_property_definition_type ON property_definition(type);

-- Add table comment
COMMENT ON TABLE property_definition IS 'Property definitions with validation rules';

-- Add column comments
COMMENT ON COLUMN property_definition.name IS 'Property name identifier';
COMMENT ON COLUMN property_definition.description IS 'Human-readable property description';
COMMENT ON COLUMN property_definition.type IS 'Property type: STRING, NUMBER, or BOOLEAN';
COMMENT ON COLUMN property_definition.required IS 'Whether this property is mandatory';
COMMENT ON COLUMN property_definition.rules_id IS 'Reference to validation rules';
