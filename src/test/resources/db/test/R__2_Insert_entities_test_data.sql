-- -----------------------------------------------------------------------
-- Sample entity instances
-- -----------------------------------------------------------------------

-- Definitions used by the JSONB array regression fixtures.
INSERT INTO property_definition (id, name, description, type, required, rules_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440090', 'stringArray', 'String array', 'STRING', false, NULL),
  ('550e8400-e29b-41d4-a716-446655440091', 'numberArray', 'Number array', 'NUMBER', false, NULL),
  ('550e8400-e29b-41d4-a716-446655440092', 'booleanArray', 'Boolean array', 'BOOLEAN', false, NULL);

INSERT INTO entity_template (id, identifier, name, description)
VALUES
  ('550e8400-e29b-41d4-a716-446655440083', 'web-api-complex', 'Complex Web API',
   'Template used for JSONB array properties');

INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440083', '550e8400-e29b-41d4-a716-446655440090'),
  ('550e8400-e29b-41d4-a716-446655440083', '550e8400-e29b-41d4-a716-446655440091'),
  ('550e8400-e29b-41d4-a716-446655440083', '550e8400-e29b-41d4-a716-446655440092');

INSERT INTO entity (id, identifier, name, template_identifier)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'web-api-1', 'Web API 1', 'web-service'),
  ('550e8400-e29b-41d4-a716-446655440101', 'web-api-2', 'Web API 2', 'web-service'),
  ('550e8400-e29b-41d4-a716-446655440118', 'web-api-3', 'Web API 3', 'web-api-complex'),
  ('550e8400-e29b-41d4-a716-446655440102', 'microservice-1', 'Microservice 1', 'microservice'),
  ('550e8400-e29b-41d4-a716-446655440103', 'batch-job-1', 'Batch Job 1', 'batch-job'),
  ('550e8400-e29b-41d4-a716-446655440104', 'frontend-app-1', 'Frontend App 1', 'frontend-app'),
  ('550e8400-e29b-41d4-a716-446655440105', 'worker-service-1', 'Worker Service 1', 'worker-service'),
  ('550e8400-e29b-41d4-a716-446655440106', 'api-gateway-1', 'API Gateway 1', 'api-gateway'),
  ('550e8400-e29b-41d4-a716-446655440107', 'database-service-1', 'Database Service 1', 'database-service'),
  ('550e8400-e29b-41d4-a716-446655440108', 'cache-service-1', 'Cache Service 1', 'cache-service'),
  ('550e8400-e29b-41d4-a716-446655440109', 'monitoring-service-1', 'Monitoring Service 1', 'monitoring-service'),
  ('550e8400-e29b-41d4-a716-446655440110', 'monitoring-service-2', 'Monitoring Service 2', 'monitoring-service'),
  ('550e8400-e29b-41d4-a716-446655440111', 'monitoring-service-3', 'Monitoring Service 3', 'monitoring-service'),
  ('550e8400-e29b-41d4-a716-446655440112', 'monitoring-service-4', 'Monitoring Service 4', 'monitoring-service'),
  ('550e8400-e29b-41d4-a716-446655440113', 'monitoring-service-5', 'Monitoring Service 5', 'monitoring-service'),
  ('550e8400-e29b-41d4-a716-446655440114', 'monitoring-service-6', 'Monitoring Service 6', 'monitoring-service'),
  ('550e8400-e29b-41d4-a716-446655440115', 'default-team', 'Default Team', 'team'),
  ('550e8400-e29b-41d4-a716-446655440116', 'test-team-required', 'Test Team Required', 'team'),
  ('550e8400-e29b-41d4-a716-446655440117', 'test-support-with-required-team', 'Test Support With Required Team', 'support');

-- Properties for default-team entity
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000007', 'applicationName', '"test-app"'),
  ('aa000000-0000-0000-0000-000000000008', 'ownerEmail', '"team@example.com"'),
  ('aa000000-0000-0000-0000-000000000009', 'environment', '"DEV"');
INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440115', 'aa000000-0000-0000-0000-000000000007'),
  ('550e8400-e29b-41d4-a716-446655440115', 'aa000000-0000-0000-0000-000000000008'),
  ('550e8400-e29b-41d4-a716-446655440115', 'aa000000-0000-0000-0000-000000000009');

-- Properties for test-team-required entity
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000010', 'applicationName', '"test-team-app"'),
  ('aa000000-0000-0000-0000-000000000011', 'ownerEmail', '"testteam@example.com"'),
  ('aa000000-0000-0000-0000-000000000012', 'environment', '"PROD"');
INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440116', 'aa000000-0000-0000-0000-000000000010'),
  ('550e8400-e29b-41d4-a716-446655440116', 'aa000000-0000-0000-0000-000000000011'),
  ('550e8400-e29b-41d4-a716-446655440116', 'aa000000-0000-0000-0000-000000000012');

-- Properties for test-support-with-required-team entity
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000013', 'applicationName', '"support-app"'),
  ('aa000000-0000-0000-0000-000000000014', 'ownerEmail', '"support@example.com"'),
  ('aa000000-0000-0000-0000-000000000015', 'environment', '"PROD"'),
  ('aa000000-0000-0000-0000-000000000016', 'version', '"1.0.0"'),
  ('aa000000-0000-0000-0000-000000000017', 'teamName', '"support-team"');
INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440117', 'aa000000-0000-0000-0000-000000000013'),
  ('550e8400-e29b-41d4-a716-446655440117', 'aa000000-0000-0000-0000-000000000014'),
  ('550e8400-e29b-41d4-a716-446655440117', 'aa000000-0000-0000-0000-000000000015'),
  ('550e8400-e29b-41d4-a716-446655440117', 'aa000000-0000-0000-0000-000000000016'),
  ('550e8400-e29b-41d4-a716-446655440117', 'aa000000-0000-0000-0000-000000000017');

-- Properties for web-api-1 (language=JAVA, environment=PROD)
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000001', 'programmingLanguage', '"JAVA"'),
  ('aa000000-0000-0000-0000-000000000002', 'environment', '"PROD"'),
  ('aa000000-0000-0000-0000-000000000005', 'port', '"8080"');

INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000001'),
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000002'),
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000005');

-- Properties for web-api-2 (language=PYTHON, environment=DEV)
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000003', 'programmingLanguage', '"PYTHON"'),
  ('aa000000-0000-0000-0000-000000000004', 'environment', '"DEV"'),
  ('aa000000-0000-0000-0000-000000000006', 'port', '"9090"');

INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000003'),
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000004'),
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000006');

-- Properties for web-api-3 (array properties, template=web-api-complex)
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000018', 'stringArray', '["JAVA","SPRING"]'),
  ('aa000000-0000-0000-0000-000000000019', 'numberArray', '[8080,9090]'),
  ('aa000000-0000-0000-0000-000000000020', 'booleanArray', '[true,false]');

INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440118', 'aa000000-0000-0000-0000-000000000018'),
  ('550e8400-e29b-41d4-a716-446655440118', 'aa000000-0000-0000-0000-000000000019'),
  ('550e8400-e29b-41d4-a716-446655440118', 'aa000000-0000-0000-0000-000000000020');

-- Relations for web-api-1 (database -> database-service, targetTemplateIdentifier = database-service)
INSERT INTO relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000001', 'database', 'database-service');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier, target_entity_uuid)
VALUES
  ('bb000000-0000-0000-0000-000000000001', 'database-service-1', '550e8400-e29b-41d4-a716-446655440107');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'bb000000-0000-0000-0000-000000000001');

-- Relations for web-api-2 (database -> cache-service, targetTemplateIdentifier = cache-service)
INSERT INTO relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000002', 'database', 'cache-service');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier, target_entity_uuid)
VALUES
  ('bb000000-0000-0000-0000-000000000002', 'cache-service-1', '550e8400-e29b-41d4-a716-446655440108');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440101', 'bb000000-0000-0000-0000-000000000002');

-- api-link relation for web-api-1 targeting microservice-1 (supports q=relation=api-link;relation.api-link.name:microservice)
INSERT INTO relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000003', 'api-link', 'microservice');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier, target_entity_uuid)
VALUES
  ('bb000000-0000-0000-0000-000000000003', 'microservice-1', '550e8400-e29b-41d4-a716-446655440102');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'bb000000-0000-0000-0000-000000000003');

-- required_team relation for test-support-with-required-team targeting test-team-required
INSERT INTO relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000006', 'required_team', 'team');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier, target_entity_uuid)
VALUES
  ('bb000000-0000-0000-0000-000000000006', 'test-team-required', '550e8400-e29b-41d4-a716-446655440116');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440117', 'bb000000-0000-0000-0000-000000000006');
