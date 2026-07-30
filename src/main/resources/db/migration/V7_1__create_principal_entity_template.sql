-- Flyway migration script: Create the "principal" entity template
-- and its associated properties and relations in the IDP-Core catalog
INSERT INTO idp_core.entity_template (id, identifier, name, description)
SELECT gen_random_uuid(),
       'principal',
       'Principal',
       'Unified identity representing authenticated actors (humans or service accounts) in the IDP-Core catalog' WHERE NOT EXISTS (SELECT 1 FROM idp_core.entity_template WHERE identifier = 'principal');

INSERT INTO idp_core.entity_template (id, identifier, name, description)
SELECT gen_random_uuid(),
       'team',
       'Team',
       'Organizational team or group for access control and collaboration' WHERE NOT EXISTS (SELECT 1 FROM idp_core.entity_template WHERE identifier = 'team');

INSERT INTO idp_core.property_rules (id, format)
SELECT gen_random_uuid(), 'EMAIL' WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_rules WHERE format = 'EMAIL');

INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(),
       'kind',
       'STRING',
       'Type of principal: HUMAN or SERVICE_ACCOUNT',
       true WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'kind');

INSERT INTO idp_core.property_definition (id, name, type, description, required, rules_id)
SELECT gen_random_uuid(), 'email', 'STRING', 'Email address (for HUMAN principals)', false, pr.id
FROM idp_core.property_rules pr
WHERE pr.format = 'EMAIL'
  AND NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'email');

INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(),
       'client_id',
       'STRING',
       'OAuth2 client identifier (for SERVICE_ACCOUNT principals)',
       false WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'client_id');

INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(),
       'origin',
       'STRING',
       'Origin system or service (for SERVICE_ACCOUNT principals)',
       false WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'origin');

INSERT INTO idp_core.entity_template_properties_definitions (entity_template_id, properties_definitions_id)
SELECT et.id, pd.id
FROM idp_core.entity_template et
         CROSS JOIN idp_core.property_definition pd
WHERE et.identifier = 'principal'
  AND pd.name IN ('kind', 'email', 'client_id', 'origin')
  AND NOT EXISTS (SELECT 1
                  FROM idp_core.entity_template_properties_definitions etpd
                  WHERE etpd.entity_template_id = et.id
                    AND etpd.properties_definitions_id = pd.id);

INSERT INTO idp_core.relation_definition (id, name, target_template_identifier, required, to_many)
SELECT gen_random_uuid(), 'member_of', 'team', false, true WHERE NOT EXISTS (SELECT 1 FROM idp_core.relation_definition WHERE name = 'member_of' AND target_template_identifier = 'team');

INSERT INTO idp_core.entity_template_relations_definitions (entity_template_id, relations_definitions_id)
SELECT et.id, rd.id
FROM idp_core.entity_template et
         CROSS JOIN idp_core.relation_definition rd
WHERE et.identifier = 'principal'
  AND rd.name = 'member_of'
  AND NOT EXISTS (SELECT 1
                  FROM idp_core.entity_template_relations_definitions etrd
                  WHERE etrd.entity_template_id = et.id
                    AND etrd.relations_definitions_id = rd.id);
