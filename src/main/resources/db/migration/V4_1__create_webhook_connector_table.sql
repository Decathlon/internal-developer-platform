CREATE TABLE webhook_connector (
                                   id UUID PRIMARY KEY,
                                   identifier VARCHAR(255) NOT NULL,
                                   title VARCHAR(255) NOT NULL,
                                   description TEXT,
                                   enabled BOOLEAN DEFAULT TRUE,
                                   mappings JSONB NOT NULL DEFAULT '[]'::jsonb,
                                   security JSONB NOT NULL,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_webhook_connector_identifier ON webhook_connector (identifier);

CREATE INDEX idx_webhook_security_type ON webhook_connector USING GIN (security);
