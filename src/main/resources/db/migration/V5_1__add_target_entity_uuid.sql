ALTER TABLE relation_target_entities 
ADD COLUMN target_entity_uuid UUID;

CREATE INDEX idx_rte_target_uuid_binary 
ON relation_target_entities (target_entity_uuid);