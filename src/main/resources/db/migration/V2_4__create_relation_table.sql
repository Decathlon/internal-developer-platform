-- Main relation table
CREATE TABLE relation (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    target_template_identifier VARCHAR(255)
);
