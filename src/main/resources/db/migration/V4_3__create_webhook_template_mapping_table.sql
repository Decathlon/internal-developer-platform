--- Create the webhook_template_mapping table to link webhook connectors with entity templates and dynamic mappings.
CREATE TABLE webhook_template_mapping
(
    id                UUID PRIMARY KEY,
    webhook_id        UUID NOT NULL,
    template_id       UUID NOT NULL,
    entity_mapping_id UUID NOT NULL,
    jslt_filter       TEXT,

    CONSTRAINT fk_webhook_connector FOREIGN KEY (webhook_id) REFERENCES webhook_connector (id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_template FOREIGN KEY (template_id) REFERENCES entity_template (id) ON DELETE RESTRICT,
    CONSTRAINT fk_entity_mapping FOREIGN KEY (entity_mapping_id) REFERENCES entity_dynamic_mapping (id) ON DELETE CASCADE

);

CREATE INDEX idx_webhook_template_mapping_webhook ON webhook_template_mapping (webhook_id);

CREATE INDEX idx_webhook_template_mapping_template ON webhook_template_mapping (template_id);

CREATE INDEX idx_webhook_template_mapping_entity_mapping ON webhook_template_mapping (entity_mapping_id);

COMMENT ON TABLE webhook_template_mapping IS 'Links webhook connectors to entity templates and dynamic mappings used for inbound webhook ingestion.';
COMMENT ON COLUMN webhook_template_mapping.id IS 'Technical UUID identifier.';
COMMENT ON COLUMN webhook_template_mapping.webhook_id IS 'FK to webhook_connector.id.';
COMMENT ON COLUMN webhook_template_mapping.template_id IS 'FK to entity_template.id.';
COMMENT ON COLUMN webhook_template_mapping.entity_mapping_id IS 'FK to entity_dynamic_mapping.id.';
COMMENT ON COLUMN webhook_template_mapping.jslt_filter IS 'JSLT filter expression used during ingestion.';
