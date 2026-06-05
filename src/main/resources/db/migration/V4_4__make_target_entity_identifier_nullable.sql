-- Transition to UUID-only storage for relation targets
-- Purpose: Remove dependency on composite (identifier + template) key and use UUID as primary storage

-- Step 1: Drop the old composite primary key that includes identifier
ALTER TABLE relation_target_entities 
DROP CONSTRAINT IF EXISTS relation_target_entities_pkey;

-- Step 2: Ensure all existing rows have UUIDs populated (data migration)
-- UPDATE relation_target_entities rte
-- SET target_entity_uuid = e.id
-- FROM entity e
-- WHERE e.identifier = rte.target_entity_identifier
--   AND e.template_identifier = rte.target_template_identifier
--   AND rte.target_entity_uuid IS NULL;

-- Step 3: Make target_entity_uuid NOT NULL (it's now the primary storage)
-- ALTER TABLE relation_target_entities 
-- ALTER COLUMN target_entity_uuid SET NOT NULL;

-- Step 4: Add new primary key using UUID only
ALTER TABLE relation_target_entities 
ADD CONSTRAINT relation_target_entities_pkey 
PRIMARY KEY (relation_id, target_entity_uuid);

-- Step 5: Make target_entity_identifier nullable (preparing for future removal)
ALTER TABLE relation_target_entities 
ALTER COLUMN target_entity_identifier DROP NOT NULL;

-- Step 7: Add index on UUID for better query performance
CREATE INDEX IF NOT EXISTS idx_relation_target_uuid 
ON relation_target_entities(target_entity_uuid);

-- Step 8: Add foreign key constraint for referential integrity
ALTER TABLE relation_target_entities 
ADD CONSTRAINT fk_relation_target_entity 
FOREIGN KEY (target_entity_uuid) 
REFERENCES entity(id) 
ON DELETE CASCADE;
