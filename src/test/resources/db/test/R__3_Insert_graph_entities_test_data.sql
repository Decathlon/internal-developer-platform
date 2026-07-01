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
--
-- All statements use ON CONFLICT DO NOTHING to make this script idempotent:
-- safe to run via Flyway on startup AND via @Sql before the graph test class.
-- -----------------------------------------------------------------------

INSERT INTO entity (id, identifier, name, template_identifier) VALUES
  ('aa000001-0000-0000-0000-000000000001', 'graph-svc-a', 'Graph Service A', 'web-service'),
  ('aa000001-0000-0000-0000-000000000002', 'graph-svc-b', 'Graph Service B', 'web-service'),
  ('aa000001-0000-0000-0000-000000000003', 'graph-svc-c', 'Graph Service C', 'web-service')
ON CONFLICT DO NOTHING;

-- Relations owned by graph-svc-a: "uses" → b, "monitors" → b
INSERT INTO relation (id, name, target_template_identifier) VALUES
  ('bb000001-0000-0000-0000-000000000001', 'uses',     'web-service'),
  ('bb000001-0000-0000-0000-000000000002', 'monitors', 'web-service')
ON CONFLICT DO NOTHING;

-- Relation owned by graph-svc-b: "uses" → c
INSERT INTO relation (id, name, target_template_identifier) VALUES
  ('bb000002-0000-0000-0000-000000000001', 'uses', 'web-service')
ON CONFLICT DO NOTHING;

-- Target entity identifiers for each relation
INSERT INTO relation_target_entities (relation_id, target_entity_identifier, target_entity_uuid) VALUES
  ('bb000001-0000-0000-0000-000000000001', 'graph-svc-b', 'aa000001-0000-0000-0000-000000000002'),  -- a -[uses]-> b
  ('bb000001-0000-0000-0000-000000000002', 'graph-svc-b', 'aa000001-0000-0000-0000-000000000002'),  -- a -[monitors]-> b
  ('bb000002-0000-0000-0000-000000000001', 'graph-svc-c', 'aa000001-0000-0000-0000-000000000003')   -- b -[uses]-> c
ON CONFLICT DO NOTHING;

-- Link relations to their owner entities
INSERT INTO entity_relations (entity_id, relation_id) VALUES
  ('aa000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001'), -- a owns "uses" relation
  ('aa000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000002'), -- a owns "monitors" relation
  ('aa000001-0000-0000-0000-000000000002', 'bb000002-0000-0000-0000-000000000001')  -- b owns "uses" relation
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------
-- Property data for graph test entities (used by the property-filter tests).
-- -----------------------------------------------------------------------

INSERT INTO property (id, name, value) VALUES
  ('cc000001-0000-0000-0000-000000000001', 'tier',    'gold'),
  ('cc000001-0000-0000-0000-000000000002', 'version', '1.0.0'),
  ('cc000001-0000-0000-0000-000000000003', 'tier',    'silver'),
  ('cc000001-0000-0000-0000-000000000004', 'version', '2.0.0'),
  ('cc000001-0000-0000-0000-000000000005', 'tier',    'bronze'),
  ('cc000001-0000-0000-0000-000000000006', 'version', '3.0.0')
ON CONFLICT DO NOTHING;

INSERT INTO entity_properties (entity_id, property_id) VALUES
  ('aa000001-0000-0000-0000-000000000001', 'cc000001-0000-0000-0000-000000000001'), -- a.tier
  ('aa000001-0000-0000-0000-000000000001', 'cc000001-0000-0000-0000-000000000002'), -- a.version
  ('aa000001-0000-0000-0000-000000000002', 'cc000001-0000-0000-0000-000000000003'), -- b.tier
  ('aa000001-0000-0000-0000-000000000002', 'cc000001-0000-0000-0000-000000000004'), -- b.version
  ('aa000001-0000-0000-0000-000000000003', 'cc000001-0000-0000-0000-000000000005'), -- c.tier
  ('aa000001-0000-0000-0000-000000000003', 'cc000001-0000-0000-0000-000000000006')  -- c.version
ON CONFLICT DO NOTHING;
