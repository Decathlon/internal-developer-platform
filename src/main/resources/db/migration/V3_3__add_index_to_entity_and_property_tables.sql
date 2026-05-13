-- Optimize property deletion by template identifier and property name

CREATE INDEX idx_entity_by_template_identifier
ON entity (template_identifier);

CREATE INDEX idx_property_name
ON property (name);
