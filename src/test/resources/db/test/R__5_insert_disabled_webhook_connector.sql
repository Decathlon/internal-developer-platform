-- Test data for disabled webhook connector
-- Purpose: validate ingestion route returns 403 when webhook exists but is disabled

INSERT INTO webhook_connector (id, identifier, name, description, enabled, security)
VALUES ('770e8400-e29b-41d4-a716-446655440099',
        'disabled-connector',
        'Disabled Connector',
        'Connector disabled for ingestion tests',
        false,
        '{
          "type": "NONE",
          "config": {}
        }'::jsonb);
