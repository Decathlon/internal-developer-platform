CREATE INDEX idx_rte_covering_outbound 
ON relation_target_entities (relation_id) 
INCLUDE (target_entity_identifier);

-- 2. Optimized Covering Index for the Inbound Track
CREATE INDEX idx_rte_covering_inbound 
ON relation_target_entities (target_entity_identifier) 
INCLUDE (relation_id);

-- 3. Composite covering index for the main Entity table lookups
CREATE INDEX idx_entity_covering_graph 
ON entity (template_identifier, identifier) 
INCLUDE (id, name);