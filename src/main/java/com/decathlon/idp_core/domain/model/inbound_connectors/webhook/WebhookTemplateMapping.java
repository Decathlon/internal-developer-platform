package com.decathlon.idp_core.domain.model.inbound_connectors.webhook;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

/// Domain model representing the mapping between a webhook event and an entity entityTemplateIdentifier.
///
/// Per the webhook_template_mapping schema:
/// - Links a webhook connector to an entity entityTemplateIdentifier for event ingestion
/// - Contains the JSLT filter to apply during transformation
/// - Includes both technical IDs (from persistence) and functional domain objects
///
/// @param id technical identifier of the mapping record
/// @param webhookConnector domain model of the associated webhook connector
/// @param entityTemplate domain model of the target entity entityTemplateIdentifier
/// @param entityDynamicMapping domain model of the dynamic mapping configuration
/// @param jsltFilter JSLT filter expression for event ingestion
public record WebhookTemplateMapping(UUID id, WebhookConnector webhookConnector,
    EntityTemplate entityTemplate, EntityDynamicMapping entityDynamicMapping, String jsltFilter) {
}
