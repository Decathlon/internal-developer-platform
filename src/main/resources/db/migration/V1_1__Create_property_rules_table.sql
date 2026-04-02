-- Create property_rules table
-- This table contains validation rules for property definitions

CREATE TABLE property_rules (
    id UUID PRIMARY KEY,
    format VARCHAR(50),
    enum_values TEXT[], -- PostgreSQL array for string array
    regex VARCHAR(500),
    max_length INTEGER,
    min_length INTEGER,
    max_value INTEGER,
    min_value INTEGER
);

-- Add table comment
COMMENT ON TABLE property_rules IS 'Validation rules for properties';

-- Add column comments
COMMENT ON COLUMN property_rules.format IS 'Format validation: URL, EMAIL, etc.';
COMMENT ON COLUMN property_rules.enum_values IS 'Array of allowed enum values';
COMMENT ON COLUMN property_rules.regex IS 'Regular expression for validation';
COMMENT ON COLUMN property_rules.max_length IS 'Maximum string length';
COMMENT ON COLUMN property_rules.min_length IS 'Minimum string length';
COMMENT ON COLUMN property_rules.max_value IS 'Maximum numeric value';
COMMENT ON COLUMN property_rules.min_value IS 'Minimum numeric value';
