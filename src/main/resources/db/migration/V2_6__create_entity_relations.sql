-- Create the entity_relations join table
CREATE TABLE entity_relations (
    entity_id UUID NOT NULL,
    relation_id UUID NOT NULL,
    PRIMARY KEY (entity_id, relation_id),
    CONSTRAINT fk_entity_relations_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_relations_relation FOREIGN KEY (relation_id) REFERENCES relation(id) ON DELETE CASCADE
);

CREATE INDEX idx_entity_relations_entity_id ON entity_relations(entity_id);
