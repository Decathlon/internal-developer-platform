CREATE TABLE webhook_template_mapping
(
    id          UUID PRIMARY KEY,
    webhook_id  UUID NOT NULL,
    template_id UUID NOT NULL,

    jslt_filter TEXT,

    CONSTRAINT fk_webhook_connector FOREIGN KEY (webhook_id) REFERENCES webhook_connector (id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_template FOREIGN KEY (template_id) REFERENCES entity_template (id) ON DELETE RESTRICT
);
