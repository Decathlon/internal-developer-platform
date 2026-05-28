-- -----------------------------------------------------------------------
-- Sample entity instances
-- -----------------------------------------------------------------------

INSERT INTO entity (id, identifier, name, template_identifier)
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


-- Add to end of R__1_Insert_test_data.sql

-- -----------------------------------------------------------------------
-- Properties for query filter tests (web-api-1 and web-api-2)
-- -----------------------------------------------------------------------

-- Properties for web-api-1 (programmingLanguage=JAVA, environment=PROD, port=8080)
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000001', 'programmingLanguage', 'JAVA'),
  ('aa000000-0000-0000-0000-000000000002', 'environment', 'PROD'),
  ('aa000000-0000-0000-0000-000000000005', 'port', '8080');

INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000001'),
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000002'),
  ('550e8400-e29b-41d4-a716-446655440100', 'aa000000-0000-0000-0000-000000000005');

-- Properties for web-api-2 (programmingLanguage=PYTHON, environment=DEV, port=9090)
INSERT INTO property (id, name, value)
VALUES
  ('aa000000-0000-0000-0000-000000000003', 'programmingLanguage', 'PYTHON'),
  ('aa000000-0000-0000-0000-000000000004', 'environment', 'DEV'),
  ('aa000000-0000-0000-0000-000000000006', 'port', '9090');

INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000003'),
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000004'),
  ('550e8400-e29b-41d4-a716-446655440101', 'aa000000-0000-0000-0000-000000000006');

-- -----------------------------------------------------------------------
-- Relations for query filter tests (web-api-1 and web-api-2)
-- -----------------------------------------------------------------------

-- database relation for web-api-1 → database-service-1
INSERT INTO relation (id, name, target_template_identifier)
VALUES ('bb000000-0000-0000-0000-000000000001', 'database', 'database-service');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier)
VALUES ('bb000000-0000-0000-0000-000000000001', 'database-service-1');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES ('550e8400-e29b-41d4-a716-446655440100', 'bb000000-0000-0000-0000-000000000001');

-- database relation for web-api-2 → cache-service-1
INSERT INTO relation (id, name, target_template_identifier)
VALUES ('bb000000-0000-0000-0000-000000000002', 'database', 'cache-service');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier)
VALUES ('bb000000-0000-0000-0000-000000000002', 'cache-service-1');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES ('550e8400-e29b-41d4-a716-446655440101', 'bb000000-0000-0000-0000-000000000002');

-- api-link relation for web-api-1 → microservice-1
INSERT INTO relation (id, name, target_template_identifier)
VALUES ('bb000000-0000-0000-0000-000000000003', 'api-link', 'microservice');

INSERT INTO relation_target_entities (relation_id, target_entity_identifier)
VALUES ('bb000000-0000-0000-0000-000000000003', 'microservice-1');

INSERT INTO entity_relations (entity_id, relation_id)
VALUES ('550e8400-e29b-41d4-a716-446655440100', 'bb000000-0000-0000-0000-000000000003');
-- -----------------------------------------------------------------------
-- Graph test data: 3-level chain of entities connected via two relation
-- types ("uses" and "monitors") for integration testing of the graph API.
--
-- Graph topology (depth-3 chain):
--   graph-svc-a --[uses]--> graph-svc-b --[uses]--> graph-svc-c
--   graph-svc-a --[monitors]--> graph-svc-b
--
-- This setup allows us to verify:
--   1. Graph traversal works at all depths (not just root level)
--   2. Relation name filtering excludes the correct edges/nodes at every depth
--   3. "uses" filter returns: a → b → c  (2 edges, 3 nodes)
--   4. "monitors" filter returns: a → b  (1 edge, 2 nodes; c not reachable)
-- -----------------------------------------------------------------------

INSERT INTO entity (id, identifier, name, template_identifier)
VALUES
  ('aa000001-0000-0000-0000-000000000001', 'graph-svc-a', 'Graph Service A', 'web-service'),
  ('aa000001-0000-0000-0000-000000000002', 'graph-svc-b', 'Graph Service B', 'web-service'),
  ('aa000001-0000-0000-0000-000000000003', 'graph-svc-c', 'Graph Service C', 'web-service');

-- Relations owned by graph-svc-a: "uses" → b, "monitors" → b
INSERT INTO relation (id, name, target_template_identifier)
VALUES
  ('bb000001-0000-0000-0000-000000000001', 'uses',     'web-service'),
  ('bb000001-0000-0000-0000-000000000002', 'monitors', 'web-service');

-- Relation owned by graph-svc-b: "uses" → c
INSERT INTO relation (id, name, target_template_identifier)
VALUES
  ('bb000002-0000-0000-0000-000000000001', 'uses', 'web-service');

-- Target entity identifiers for each relation
INSERT INTO relation_target_entities (relation_id, target_entity_identifier)
VALUES
  ('bb000001-0000-0000-0000-000000000001', 'graph-svc-b'),  -- a -[uses]-> b
  ('bb000001-0000-0000-0000-000000000002', 'graph-svc-b'),  -- a -[monitors]-> b
  ('bb000002-0000-0000-0000-000000000001', 'graph-svc-c');  -- b -[uses]-> c

-- Link relations to their owner entities
INSERT INTO entity_relations (entity_id, relation_id)
VALUES
  ('aa000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001'), -- a owns "uses" relation
  ('aa000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000002'), -- a owns "monitors" relation
  ('aa000001-0000-0000-0000-000000000002', 'bb000002-0000-0000-0000-000000000001'); -- b owns "uses" relation

-- -----------------------------------------------------------------------
-- Property data for graph test entities (used by the property-filter tests).
--
-- Each graph entity gets two properties: "tier" and "version".
-- This lets us verify:
--   1. No filter  → both properties appear in node data
--   2. Filter "tier"           → only tier present, version absent
--   3. Filter "tier"+"version" → both present
--   4. Filter "non-existent"   → data field omitted entirely (NON_EMPTY)
-- -----------------------------------------------------------------------

INSERT INTO property (id, name, value)
VALUES
  -- graph-svc-a
  ('cc000001-0000-0000-0000-000000000001', 'tier',    'gold'),
  ('cc000001-0000-0000-0000-000000000002', 'version', '1.0.0'),
  -- graph-svc-b
  ('cc000001-0000-0000-0000-000000000003', 'tier',    'silver'),
  ('cc000001-0000-0000-0000-000000000004', 'version', '2.0.0'),
  -- graph-svc-c
  ('cc000001-0000-0000-0000-000000000005', 'tier',    'bronze'),
  ('cc000001-0000-0000-0000-000000000006', 'version', '3.0.0');

INSERT INTO entity_properties (entity_id, property_id)
VALUES
  ('aa000001-0000-0000-0000-000000000001', 'cc000001-0000-0000-0000-000000000001'), -- a.tier
  ('aa000001-0000-0000-0000-000000000001', 'cc000001-0000-0000-0000-000000000002'), -- a.version
  ('aa000001-0000-0000-0000-000000000002', 'cc000001-0000-0000-0000-000000000003'), -- b.tier
  ('aa000001-0000-0000-0000-000000000002', 'cc000001-0000-0000-0000-000000000004'), -- b.version
  ('aa000001-0000-0000-0000-000000000003', 'cc000001-0000-0000-0000-000000000005'), -- c.tier
  ('aa000001-0000-0000-0000-000000000003', 'cc000001-0000-0000-0000-000000000006'); -- c.version