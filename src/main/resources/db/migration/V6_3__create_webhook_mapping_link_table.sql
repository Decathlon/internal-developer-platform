--- Create the webhook_mapping_link table to link webhook connectors with entity templates and dynamic mappings.
CREATE TABLE webhook_mapping_link
(
    webhook_id        UUID NOT NULL,
    entity_mapping_id UUID NOT NULL,
    jslt_filter       TEXT,
    PRIMARY KEY (webhook_id, entity_mapping_id),
    CONSTRAINT fk_webhook_connector FOREIGN KEY (webhook_id) REFERENCES webhook_connector (id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_mapping FOREIGN KEY (entity_mapping_id) REFERENCES entity_dynamic_mapping (id) ON DELETE CASCADE

);

CREATE INDEX idx_webhook_template_mapping_webhook ON webhook_mapping_link (webhook_id);


CREATE INDEX idx_webhook_template_mapping_entity_mapping ON webhook_mapping_link (entity_mapping_id);

COMMENT ON TABLE webhook_mapping_link IS 'Links webhook connectors to dynamic mappings used for inbound webhook ingestion.';
COMMENT ON COLUMN webhook_mapping_link.webhook_id IS 'FK to webhook_connector.id.';
COMMENT ON COLUMN webhook_mapping_link.entity_mapping_id IS 'FK to entity_dynamic_mapping.id.';
COMMENT ON COLUMN webhook_mapping_link.jslt_filter IS 'JSLT filter expression used during ingestion.';
