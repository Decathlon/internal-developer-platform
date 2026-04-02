-- Flyway migration script to create Entity-Property join table

CREATE TABLE entity_properties (
    entity_id UUID NOT NULL,
    property_id UUID NOT NULL,
    PRIMARY KEY (entity_id, property_id),
    CONSTRAINT fk_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_property FOREIGN KEY (property_id) REFERENCES property(id) ON DELETE CASCADE
);

CREATE INDEX idx_entity_properties_entity_id ON entity_properties(entity_id);
