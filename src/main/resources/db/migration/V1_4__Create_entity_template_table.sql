-- Create entity_template table
-- This table contains the main template entity definitions

CREATE TABLE entity_template (
    id UUID PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- Create index for better performance
CREATE INDEX idx_entity_template_identifier ON entity_template(identifier);

-- Add table comment
COMMENT ON TABLE entity_template IS 'Main template entity containing template definitions';

-- Add column comments
COMMENT ON COLUMN entity_template.id IS 'Unique UUID identifier for the template';
COMMENT ON COLUMN entity_template.identifier IS 'Unique string identifier for the template';
COMMENT ON COLUMN entity_template.description IS 'Human-readable template description';
