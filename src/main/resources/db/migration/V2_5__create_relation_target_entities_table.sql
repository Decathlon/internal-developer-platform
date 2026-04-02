-- Join table for target entity identifiers (ElementCollection)
CREATE TABLE relation_target_entities (
    relation_id UUID NOT NULL,
    target_entity_identifier VARCHAR(255) NOT NULL,
    PRIMARY KEY (relation_id, target_entity_identifier),
    CONSTRAINT fk_relation FOREIGN KEY (relation_id) REFERENCES relation(id) ON DELETE CASCADE
);
