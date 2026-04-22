-- Sample data for IDP Core domain models - Enhanced with 10 templates

-- Clear existing data (for repeatable migrations)
DELETE FROM entity_template_relations_definitions;
DELETE FROM entity_template_properties_definitions;
DELETE FROM entity_template;
DELETE FROM relation_definition;
DELETE FROM property_definition;
DELETE FROM property_rules;

-- Insert comprehensive property rules
INSERT INTO property_rules (id, format, enum_values, regex, max_length, min_length, max_value, min_value) VALUES
-- Email validation rule
('550e8400-e29b-41d4-a716-446655440001', 'EMAIL', NULL, '^[A-Za-z0-9+_.-]+@(.+)$', 100, 5, NULL, NULL),
-- Environment enum rule
('550e8400-e29b-41d4-a716-446655440002', NULL, ARRAY['DEV', 'STAGING', 'PROD'], NULL, NULL, NULL, NULL, NULL),
-- Application name pattern rule
('550e8400-e29b-41d4-a716-446655440003', NULL, NULL, '^[a-zA-Z0-9-]+$', 50, 3, NULL, NULL),
-- URL validation rule
('550e8400-e29b-41d4-a716-446655440004', 'URL', NULL, '^https?://.*', 255, 8, NULL, NULL),
-- Port number rule
('550e8400-e29b-41d4-a716-446655440005', NULL, NULL, NULL, NULL, NULL, 65535, 1024),
-- Instance count rule
('550e8400-e29b-41d4-a716-446655440006', NULL, NULL, NULL, NULL, NULL, 100, 1),
-- Memory size rule (MB)
('550e8400-e29b-41d4-a716-446655440007', NULL, NULL, NULL, NULL, NULL, 8192, 128),
-- CPU cores rule
('550e8400-e29b-41d4-a716-446655440008', NULL, NULL, NULL, NULL, NULL, 32, 1),
-- Version pattern rule
('550e8400-e29b-41d4-a716-446655440009', NULL, NULL, '^[0-9]+\.[0-9]+\.[0-9]+$', 20, 5, NULL, NULL),
-- Language enum rule
('550e8400-e29b-41d4-a716-446655440010', NULL, ARRAY['JAVA', 'PYTHON', 'NODEJS', 'GOLANG', 'DOTNET'], NULL, NULL, NULL, NULL, NULL),
-- Database type enum rule
('550e8400-e29b-41d4-a716-446655440011', NULL, ARRAY['POSTGRESQL', 'MYSQL', 'MONGODB', 'REDIS', 'ELASTICSEARCH'], NULL, NULL, NULL, NULL, NULL),
-- Protocol enum rule
('550e8400-e29b-41d4-a716-446655440012', NULL, ARRAY['HTTP', 'HTTPS', 'TCP', 'UDP', 'GRPC'], NULL, NULL, NULL, NULL, NULL),
-- Log level enum rule
('550e8400-e29b-41d4-a716-446655440013', NULL, ARRAY['DEBUG', 'INFO', 'WARN', 'ERROR'], NULL, NULL, NULL, NULL, NULL),
-- Team name pattern rule
('550e8400-e29b-41d4-a716-446655440014', NULL, NULL, '^[a-zA-Z0-9-_]+$', 30, 2, NULL, NULL);

-- Insert diverse property definitions
INSERT INTO property_definition (id, name, description, type, required, rules_id) VALUES
-- Basic application properties
('550e8400-e29b-41d4-a716-446655440020', 'applicationName', 'Name of the application', 'STRING', true, '550e8400-e29b-41d4-a716-446655440003'),
('550e8400-e29b-41d4-a716-446655440021', 'ownerEmail', 'Email address of the application owner', 'STRING', true, '550e8400-e29b-41d4-a716-446655440001'),
('550e8400-e29b-41d4-a716-446655440022', 'environment', 'Target environment for deployment', 'STRING', true, '550e8400-e29b-41d4-a716-446655440002'),
('550e8400-e29b-41d4-a716-446655440023', 'version', 'Application version', 'STRING', true, '550e8400-e29b-41d4-a716-446655440009'),
('550e8400-e29b-41d4-a716-446655440024', 'teamName', 'Name of the owning team', 'STRING', true, '550e8400-e29b-41d4-a716-446655440014'),

-- Web service specific properties
('550e8400-e29b-41d4-a716-446655440025', 'baseUrl', 'Base URL for the service', 'STRING', true, '550e8400-e29b-41d4-a716-446655440004'),
('550e8400-e29b-41d4-a716-446655440026', 'port', 'Service port number', 'NUMBER', true, '550e8400-e29b-41d4-a716-446655440005'),
('550e8400-e29b-41d4-a716-446655440027', 'protocol', 'Communication protocol', 'STRING', true, '550e8400-e29b-41d4-a716-446655440012'),
('550e8400-e29b-41d4-a716-446655440028', 'isPublic', 'Whether the service is publicly accessible', 'BOOLEAN', false, NULL),
('550e8400-e29b-41d4-a716-446655440029', 'healthCheckPath', 'Health check endpoint path', 'STRING', false, NULL),

-- Infrastructure properties
('550e8400-e29b-41d4-a716-446655440030', 'maxInstances', 'Maximum number of instances', 'NUMBER', false, '550e8400-e29b-41d4-a716-446655440006'),
('550e8400-e29b-41d4-a716-446655440031', 'minInstances', 'Minimum number of instances', 'NUMBER', false, '550e8400-e29b-41d4-a716-446655440006'),
('550e8400-e29b-41d4-a716-446655440032', 'memoryLimit', 'Memory limit in MB', 'NUMBER', false, '550e8400-e29b-41d4-a716-446655440007'),
('550e8400-e29b-41d4-a716-446655440033', 'cpuLimit', 'CPU limit in cores', 'NUMBER', false, '550e8400-e29b-41d4-a716-446655440008'),
('550e8400-e29b-41d4-a716-446655440034', 'storageSize', 'Storage size in GB', 'NUMBER', false, NULL),

-- Development properties
('550e8400-e29b-41d4-a716-446655440035', 'programmingLanguage', 'Programming language used', 'STRING', true, '550e8400-e29b-41d4-a716-446655440010'),
('550e8400-e29b-41d4-a716-446655440036', 'framework', 'Framework or runtime used', 'STRING', false, NULL),
('550e8400-e29b-41d4-a716-446655440037', 'buildTool', 'Build tool used', 'STRING', false, NULL),
('550e8400-e29b-41d4-a716-446655440038', 'testCoverage', 'Minimum test coverage percentage', 'NUMBER', false, NULL),
('550e8400-e29b-41d4-a716-446655440039', 'logLevel', 'Application log level', 'STRING', false, '550e8400-e29b-41d4-a716-446655440013'),

-- Data properties
('550e8400-e29b-41d4-a716-446655440040', 'databaseType', 'Type of database used', 'STRING', false, '550e8400-e29b-41d4-a716-446655440011'),
('550e8400-e29b-41d4-a716-446655440041', 'connectionPoolSize', 'Database connection pool size', 'NUMBER', false, NULL),
('550e8400-e29b-41d4-a716-446655440042', 'enableCaching', 'Whether caching is enabled', 'BOOLEAN', false, NULL),
('550e8400-e29b-41d4-a716-446655440043', 'backupRequired', 'Whether backup is required', 'BOOLEAN', false, NULL),
('550e8400-e29b-41d4-a716-446655440044', 'dataRetentionDays', 'Data retention period in days', 'NUMBER', false, NULL);

-- Insert diverse relation definitions
INSERT INTO relation_definition (id, name, target_entity_identifier, required, to_many) VALUES
-- Service dependencies
('550e8400-e29b-41d4-a716-446655440050', 'dependencies', 'service', false, true),
('550e8400-e29b-41d4-a716-446655440051', 'upstream_services', 'service', false, true),
('550e8400-e29b-41d4-a716-446655440052', 'downstream_services', 'service', false, true),

-- Data relationships
('550e8400-e29b-41d4-a716-446655440053', 'database', 'database', false, false),
('550e8400-e29b-41d4-a716-446655440054', 'cache', 'cache', false, false),
('550e8400-e29b-41d4-a716-446655440055', 'message_queue', 'queue', false, true),
('550e8400-e29b-41d4-a716-446655440056', 'search_engine', 'search', false, false),

-- Infrastructure relationships
('550e8400-e29b-41d4-a716-446655440057', 'networks', 'network', false, true),
('550e8400-e29b-41d4-a716-446655440058', 'load_balancer', 'loadbalancer', false, false),
('550e8400-e29b-41d4-a716-446655440059', 'monitoring', 'monitoring', false, false),
('550e8400-e29b-41d4-a716-446655440060', 'logging', 'logging', false, false),

-- Security relationships
('550e8400-e29b-41d4-a716-446655440061', 'secrets', 'secret', false, true),
('550e8400-e29b-41d4-a716-446655440062', 'certificates', 'certificate', false, true),
('550e8400-e29b-41d4-a716-446655440063', 'auth_provider', 'auth', false, false),

-- External relationships
('550e8400-e29b-41d4-a716-446655440064', 'external_apis', 'external_api', false, true),
('550e8400-e29b-41d4-a716-446655440065', 'file_storage', 'storage', false, false);

-- Insert 10 diverse entity templates
INSERT INTO entity_template (id, identifier, name, description) VALUES
('550e8400-e29b-41d4-a716-446655440070', 'web-service', 'Web Service', 'Template for REST API web services'),
('550e8400-e29b-41d4-a716-446655440071', 'microservice', 'Microservice', 'Template for microservice applications'),
('550e8400-e29b-41d4-a716-446655440072', 'batch-job', 'Batch Job', 'Template for batch processing jobs'),
('550e8400-e29b-41d4-a716-446655440073', 'data-pipeline', 'Data Pipeline', 'Template for data processing pipelines'),
('550e8400-e29b-41d4-a716-446655440074', 'frontend-app', 'Frontend Application', 'Template for frontend applications'),
('550e8400-e29b-41d4-a716-446655440075', 'worker-service', 'Worker Service', 'Template for background worker services'),
('550e8400-e29b-41d4-a716-446655440076', 'api-gateway', 'API Gateway', 'Template for API gateway services'),
('550e8400-e29b-41d4-a716-446655440077', 'database-service', 'Database Service', 'Template for database services'),
('550e8400-e29b-41d4-a716-446655440078', 'cache-service', 'Cache Service', 'Template for caching services'),
('550e8400-e29b-41d4-a716-446655440079', 'monitoring-service', 'Monitoring Service', 'Template for monitoring and observability services');

-- Link web-service template (comprehensive web API)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440023'), -- version
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440024'), -- teamName
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440025'), -- baseUrl
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440026'), -- port
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440027'), -- protocol
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440028'), -- isPublic
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440029'), -- healthCheckPath
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440035'), -- programmingLanguage
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440036'); -- framework

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440053'), -- database
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440054'), -- cache
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440057'), -- networks
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440059'), -- monitoring
('550e8400-e29b-41d4-a716-446655440070', '550e8400-e29b-41d4-a716-446655440061'); -- secrets

-- Link microservice template (lightweight service)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440023'), -- version
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440026'), -- port
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440030'), -- maxInstances
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440031'), -- minInstances
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440032'), -- memoryLimit
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440035'); -- programmingLanguage

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440050'), -- dependencies
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440055'), -- message_queue
('550e8400-e29b-41d4-a716-446655440071', '550e8400-e29b-41d4-a716-446655440059'); -- monitoring

-- Link batch-job template (data processing)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440024'), -- teamName
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440032'), -- memoryLimit
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440033'), -- cpuLimit
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440035'), -- programmingLanguage
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440044'); -- dataRetentionDays

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440053'), -- database
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440065'), -- file_storage
('550e8400-e29b-41d4-a716-446655440072', '550e8400-e29b-41d4-a716-446655440060'); -- logging

-- Link data-pipeline template (ETL/data processing)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440040'), -- databaseType
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440041'), -- connectionPoolSize
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440043'), -- backupRequired
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440044'); -- dataRetentionDays

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440053'), -- database
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440056'), -- search_engine
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440055'), -- message_queue
('550e8400-e29b-41d4-a716-446655440073', '550e8400-e29b-41d4-a716-446655440065'); -- file_storage

-- Link frontend-app template (UI application)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440025'), -- baseUrl
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440028'), -- isPublic
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440035'), -- programmingLanguage
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440036'); -- framework

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440050'), -- dependencies
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440063'), -- auth_provider
('550e8400-e29b-41d4-a716-446655440074', '550e8400-e29b-41d4-a716-446655440062'); -- certificates

-- Link worker-service template (background processing)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440030'), -- maxInstances
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440032'), -- memoryLimit
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440035'), -- programmingLanguage
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440039'); -- logLevel

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440055'), -- message_queue
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440053'), -- database
('550e8400-e29b-41d4-a716-446655440075', '550e8400-e29b-41d4-a716-446655440060'); -- logging

-- Link api-gateway template (gateway service)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440026'), -- port
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440027'), -- protocol
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440028'), -- isPublic
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440030'); -- maxInstances

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440051'), -- upstream_services
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440058'), -- load_balancer
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440059'), -- monitoring
('550e8400-e29b-41d4-a716-446655440076', '550e8400-e29b-41d4-a716-446655440063'); -- auth_provider

-- Link database-service template (data service)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440040'), -- databaseType
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440041'), -- connectionPoolSize
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440034'), -- storageSize
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440043'); -- backupRequired

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440052'), -- downstream_services
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440061'), -- secrets
('550e8400-e29b-41d4-a716-446655440077', '550e8400-e29b-41d4-a716-446655440059'); -- monitoring

-- Link cache-service template (caching service)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440026'), -- port
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440032'), -- memoryLimit
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440042'); -- enableCaching

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440052'), -- downstream_services
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440057'), -- networks
('550e8400-e29b-41d4-a716-446655440078', '550e8400-e29b-41d4-a716-446655440059'); -- monitoring

-- Link monitoring-service template (observability service)
INSERT INTO entity_template_properties_definitions (entity_template_id, properties_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440020'), -- applicationName
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440021'), -- ownerEmail
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440022'), -- environment
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440025'), -- baseUrl
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440034'), -- storageSize
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440044'), -- dataRetentionDays
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440028'); -- isPublic

INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440051'), -- upstream_services
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440053'), -- database
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440057'), -- networks
('550e8400-e29b-41d4-a716-446655440079', '550e8400-e29b-41d4-a716-446655440064'); -- external_apis
