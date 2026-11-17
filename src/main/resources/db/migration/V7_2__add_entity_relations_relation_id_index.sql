-- Inbound graph traversal joins entity_relations on relation_id:
--   relation_target_entities (by target_entity_uuid) -> entity_relations (by relation_id) -> entity
-- The only indexes on entity_relations start with entity_id (primary key and idx_entity_relations_entity_id),
-- so that join fell back to a sequential scan for every node of the traversal frontier.
-- The recursive graph query runs on every entity list page, so the scan was paid on each list call.
CREATE INDEX IF NOT EXISTS idx_entity_relations_relation_id ON entity_relations (relation_id);
