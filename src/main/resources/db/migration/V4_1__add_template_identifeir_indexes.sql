CREATE INDEX idx_entity_graph_lookup 
ON entity (template_identifier, identifier);

-- 2. Fixes the Inbound bottleneck: 
-- Speeds up reverse-searching who points to a specific target text string
CREATE INDEX idx_rte_inbound_lookup 
ON relation_target_entities (target_entity_identifier);