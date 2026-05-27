-- Insert sample entities into idp_core.entity
INSERT INTO idp_core.entity (id, identifier, name, template_identifier)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'web-api-1', 'Web API 1', 'web-service'),
  ('550e8400-e29b-41d4-a716-446655440101', 'web-api-2', 'Web API 2', 'web-service'),
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
  ('550e8400-e29b-41d4-a716-446655440114', 'monitoring-service-6', 'Monitoring Service 6', 'monitoring-service');

-- Properties for web-api-1 (language=JAVA, environment=PROD)
INSERT INTO idp_core.property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000001', 'programmingLanguage', 'JAVA'),
  ('aa000000-0000-0000-0000-000000000002', 'environment', 'PROD'),
  ('aa000000-0000-0000-0000-000000000005', 'port', '8080');
INSERT INTO idp_core.entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000001'),
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000002'),
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000005');

-- Properties for web-api-2 (language=PYTHON, environment=DEV)
INSERT INTO idp_core.property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000003', 'programmingLanguage', 'PYTHON'),
  ('aa000000-0000-0000-0000-000000000004', 'environment', 'DEV'),
  ('aa000000-0000-0000-0000-000000000006', 'port', '9090');
INSERT INTO idp_core.entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000003'),
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000004'),
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000006');

-- Relations for web-api-1 (database -> database-service, targetTemplateIdentifier = database-service)
INSERT INTO idp_core.relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000001', 'database', 'database-service');
INSERT INTO idp_core.relation_target_entities (relation_id, target_entity_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000001', 'database-service-1');
INSERT INTO idp_core.entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'bb000000-0000-0000-0000-000000000001');

-- Relations for web-api-2 (database -> cache-service, targetTemplateIdentifier = cache-service)
INSERT INTO idp_core.relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000002', 'database', 'cache-service');
INSERT INTO idp_core.relation_target_entities (relation_id, target_entity_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000002', 'cache-service-1');
INSERT INTO idp_core.entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440101', 'bb000000-0000-0000-0000-000000000002');

-- api-link relation for web-api-1 targeting microservice-1 (supports q=relation=api-link;relation.api-link.name:microservice)
INSERT INTO idp_core.relation (id, name, target_template_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000003', 'api-link', 'microservice');
INSERT INTO idp_core.relation_target_entities (relation_id, target_entity_identifier)
VALUES
  ('bb000000-0000-0000-0000-000000000003', 'microservice-1');
INSERT INTO idp_core.entity_relations (entity_id, relation_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'bb000000-0000-0000-0000-000000000003');
