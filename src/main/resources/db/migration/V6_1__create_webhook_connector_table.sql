-- Purpose: introduce webhook connector and relational mapping model to entity entityTemplateIdentifier and dynamic mapping for flexible webhook configuration and management
CREATE TABLE webhook_connector
(
    id          UUID PRIMARY KEY,
    identifier  VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    enabled     BOOLEAN      NOT NULL    DEFAULT FALSE,
    security    JSONB        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Unique index on identifier is already created by the UNIQUE constraint above.
--create index for better performance on security field which is JSONB type, using GIN index for efficient querying
CREATE INDEX idx_webhook_security_type ON webhook_connector USING GIN (security);
