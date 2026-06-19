-- Test data for Webhook Connectors
-- Purpose: provide pre-configured webhook connectors for integration testing

-- Clear existing data
DELETE
FROM webhook_template_mapping;
DELETE
FROM webhook_connector;
DELETE
FROM entity_dynamic_mapping;

-- Webhook Connector 1: GitHub Connector (HMAC_SHA256)
INSERT INTO webhook_connector (id, identifier, title, description, enabled, security)
VALUES ('770e8400-e29b-41d4-a716-446655440001',
        'github-dora-connector',
        'GitHub Connector',
        'Receives events from GitHub with HMAC validation',
        true,
        '{
          "type": "HMAC_SHA256",
          "config": {
            "header_name": "X-Hub-Signature-256",
            "secret_alias": "GITHUB_SECRET",
            "prefix": "sha256="
          }
        }'::jsonb);

-- Webhook Connector 2: Simple Token Connector (STATIC_TOKEN)
INSERT INTO webhook_connector (id, identifier, title, description, enabled, security)
VALUES ('770e8400-e29b-41d4-a716-446655440002',
        'token-connector',
        'Token Connector',
        'Simple connector with static token',
        true,
        '{
          "type": "STATIC_TOKEN",
          "config": {
            "header_name": "X-Auth-Token",
            "secret_alias": "WEBHOOK_TOKEN"
          }
        }'::jsonb);

-- Webhook Connector 3: Public Connector (NONE)
INSERT INTO webhook_connector (id, identifier, title, description, enabled, security)
VALUES ('770e8400-e29b-41d4-a716-446655440003',
        'public-connector',
        'Public Connector',
        'Open connector for testing',
        true,
        '{
          "type": "NONE",
          "config": {}
        }'::jsonb);

-- Dynamic Mapping for GitHub Connector
INSERT INTO entity_dynamic_mapping (id, identifier, template_identifier, filter, name, description, entity_identifier, entity_title, properties, relations)
VALUES ('880e8400-e29b-41d4-a716-446655440001',
        'microservice-mapping',
        'microservice',
        '.action == "pushed"',
        'Microservice Mapping',
        'Mapping for microservice entities based on GitHub push events',
        '.repository.full_name',
        '.repository.name',
        '{"applicationName": ".repository.name", "programmingLanguage": ".repository.language"}',
        '{}');

-- Webhook Template Mapping for GitHub Connector
INSERT INTO webhook_template_mapping (id, webhook_id, template_id, entity_mapping_id, jslt_filter)
VALUES ('990e8400-e29b-41d4-a716-446655440001',
        '770e8400-e29b-41d4-a716-446655440001',
        '550e8400-e29b-41d4-a716-446655440071', -- microservice template
        '880e8400-e29b-41d4-a716-446655440001',
        '.action == "pushed"');
