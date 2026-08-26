package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class IngestionProcessor {

  private final MappingEnginePort mappingEngine;
  private final EntityService entityService;

  /// Ingests a webhook payload by applying all configured mappings.
  ///
  /// For each mapping in the webhook connector:
  /// 1. Map the raw payload to a domain Entity using the JSLT engine
  /// 2. Persist the entity via the domain service (handles validation,
  /// deduplication)
  /// 3. Log success or skip if mapping returns null (filtered out)
  ///
  /// **Fail-Fast Strategy:** If any mapping throws an exception, the entire
  /// ingestion fails. Partial ingestion is avoided to maintain data consistency.
  ///
  /// @param webhookConnectorConfiguration the connector with mapping definitions
  /// @param payload the raw JSON payload from the webhook
  public void ingest(String payload, WebhookConnector webhookConnectorConfiguration) {
    log.info("Starting ingestion for webhook connector: {}",
        webhookConnectorConfiguration.identifier());

    webhookConnectorConfiguration.mappings().forEach(mapping -> applyMapping(payload, mapping));

    log.info("Completed ingestion for webhook connector: {}",
        webhookConnectorConfiguration.identifier());
  }

  /// Applies a single mapping to the payload and persists the resulting entity.
  ///
  /// Skips silently if the mapping filter excludes the payload (returns null).
  ///
  /// @param payload the raw JSON payload from the webhook
  /// @param mapping the mapping definition to apply
  private void applyMapping(String payload, EntityDynamicMapping mapping) {
    log.debug("Applying mapping: {} to template: {}", mapping.identifier(),
        mapping.entityTemplateIdentifier());

    // Map the raw payload to a domain entity using JSLT expressions
    Entity entity = mappingEngine.mapToEntity(payload, mapping);

    // Skip if the mapping filter excluded this payload (returned null)
    if (entity == null) {
      log.debug("Mapping filter excluded payload for template: {}",
          mapping.entityTemplateIdentifier());
      return;
    }

    // Persist the mapped entity via the domain service
    entityService.createEntity(entity);

    log.info("Successfully ingested entity: {} for template: {}", entity.identifier(),
        entity.templateIdentifier());
  }
}
