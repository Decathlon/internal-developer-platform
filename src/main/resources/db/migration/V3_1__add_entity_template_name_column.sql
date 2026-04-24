-- Add name column to entity_template table

ALTER TABLE entity_template
    ADD COLUMN name VARCHAR(255) UNIQUE NOT NULL;

-- Create index for better performance
CREATE INDEX idx_entity_template_name ON entity_template(name);

-- Add column comments
COMMENT ON COLUMN entity_template.name IS 'User-friendly display name for the entity template';
