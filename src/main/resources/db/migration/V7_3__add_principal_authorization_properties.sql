-- Flyway migration script: add principal identity and authorization properties
-- Purpose: Store the stable provider UUID and the administrator flag used by
-- global authorization. JIT provisioning defaults is_admin to false; the user
-- ingestion webhook may set it to true.

INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(), 'uuid', 'STRING',
       'Stable identity-provider UUID for a principal', false
WHERE NOT EXISTS (
    SELECT 1 FROM idp_core.property_definition WHERE name = 'uuid'
);

INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(), 'is_admin', 'BOOLEAN',
       'Whether the principal has global administrator permissions', false
WHERE NOT EXISTS (
    SELECT 1 FROM idp_core.property_definition WHERE name = 'is_admin'
);

INSERT INTO idp_core.entity_template_properties_definitions
    (entity_template_id, properties_definitions_id)
SELECT et.id, pd.id
FROM idp_core.entity_template et
JOIN idp_core.property_definition pd
  ON pd.name IN ('uuid', 'is_admin')
WHERE et.identifier = 'principal'
  AND NOT EXISTS (
      SELECT 1
      FROM idp_core.entity_template_properties_definitions link
      WHERE link.entity_template_id = et.id
        AND link.properties_definitions_id = pd.id
  );
