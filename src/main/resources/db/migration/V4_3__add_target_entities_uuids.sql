-- Step 1: Add new UUID column
ALTER TABLE relation_target_entities 
ADD COLUMN target_entity_uuid UUID;

-- Step 2: Populate UUIDs from existing identifiers
UPDATE relation_target_entities rte
SET target_entity_uuid = e.id
FROM entity e
WHERE e.identifier = rte.target_entity_identifier