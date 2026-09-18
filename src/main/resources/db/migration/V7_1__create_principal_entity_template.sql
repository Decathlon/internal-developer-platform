-- Flyway migration script: Create the "principal" entity template
-- and its associated properties and relations in the IDP-Core catalog
INSERT INTO idp_core.entity_template (id, identifier, name, description)
SELECT gen_random_uuid(),
       'principal',
       'Principal',
       'Unified identity representing authenticated actors (humans or service accounts) in the IDP-Core catalog'
    WHERE NOT EXISTS (SELECT 1 FROM idp_core.entity_template WHERE identifier = 'principal');

INSERT INTO idp_core.entity_template (id, identifier, name, description)
SELECT gen_random_uuid(),
       'supportgroup',
       'SupportGroup',
       'Support Group'
    WHERE NOT EXISTS (SELECT 1 FROM idp_core.entity_template WHERE identifier = 'supportgroup');

INSERT INTO idp_core.property_rules (id, format, enum_values)
SELECT gen_random_uuid(), 'ENUM', ARRAY['HUMAN', 'SERVICE_ACCOUNT']
WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_rules WHERE enum_values = ARRAY['HUMAN', 'SERVICE_ACCOUNT']);

INSERT INTO idp_core.property_rules (id, format)
SELECT gen_random_uuid(), 'EMAIL'
WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_rules WHERE format = 'EMAIL');

-- Property: kind
INSERT INTO idp_core.property_definition (id, name, type, description, required, rules_id)
SELECT gen_random_uuid(),
       'kind',
       'STRING',
       'Kind of principal',
       true,
       (SELECT pr.id FROM idp_core.property_rules pr WHERE pr.enum_values = ARRAY['HUMAN', 'SERVICE_ACCOUNT'] ORDER BY pr.id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'kind');

INSERT INTO idp_core.property_definition (id, name, type, description, required, rules_id)
SELECT gen_random_uuid(), 'email', 'STRING', 'Email address (for HUMAN principals)', false,
       (SELECT pr.id FROM idp_core.property_rules pr WHERE pr.format = 'EMAIL' ORDER BY pr.id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'email');

-- Property: is_admin
INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(), 'is_admin', 'BOOLEAN', 'is_admin', false
    WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'is_admin');

-- Property: is_digital_teammate
INSERT INTO idp_core.property_definition (id, name, type, description, required)
SELECT gen_random_uuid(), 'is_digital_teammate', 'BOOLEAN', 'is_digital_teammate', false
    WHERE NOT EXISTS (SELECT 1 FROM idp_core.property_definition WHERE name = 'is_digital_teammate');

INSERT INTO idp_core.entity_template_properties_definitions (entity_template_id, properties_definitions_id)
SELECT et.id, pd.id
FROM idp_core.entity_template et
         CROSS JOIN idp_core.property_definition pd
WHERE et.identifier = 'principal'
  AND pd.name IN ('kind', 'email', 'is_admin', 'is_digital_teammate')
  AND NOT EXISTS (SELECT 1
                  FROM idp_core.entity_template_properties_definitions etpd
                  WHERE etpd.entity_template_id = et.id
                    AND etpd.properties_definitions_id = pd.id);

-- define relation name
INSERT INTO idp_core.relation_definition (id, name, target_template_identifier, required, to_many)
SELECT gen_random_uuid(), 'principal-member_of-supportgroup', 'supportgroup', false, true
    WHERE NOT EXISTS (SELECT 1 FROM idp_core.relation_definition WHERE name = 'principal-member_of-supportgroup' AND target_template_identifier = 'supportgroup');

INSERT INTO idp_core.entity_template_relations_definitions (entity_template_id, relations_definitions_id)
SELECT et.id, rd.id
FROM idp_core.entity_template et
         CROSS JOIN idp_core.relation_definition rd
WHERE et.identifier = 'principal'
  AND rd.name = 'principal-member_of-supportgroup'
  AND NOT EXISTS (SELECT 1
                  FROM idp_core.entity_template_relations_definitions etrd
                  WHERE etrd.entity_template_id = et.id
                    AND etrd.relations_definitions_id = rd.id);
