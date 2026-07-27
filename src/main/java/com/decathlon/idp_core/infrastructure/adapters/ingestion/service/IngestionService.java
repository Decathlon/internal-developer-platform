package com.decathlon.idp_core.infrastructure.adapters.ingestion.service;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionService {

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

    webhookConnectorConfiguration.mappings().forEach(mapping -> {
      log.debug("Applying mapping: {} to template: {}", mapping.identifier(),
          mapping.entityTemplateIdentifier());

      try {
        // Map the raw payload to a domain entity using JSLT expressions
        Entity entity = mappingEngine.mapToEntity(payload, mapping);

        // Skip if the mapping filter excluded this payload (returned null)
        if (entity == null) {
          log.debug("Mapping filter excluded payload for template: {}",
              mapping.entityTemplateIdentifier());
          return;
        }

        // Persist the mapped entity via the domain service
        // (handles validation, deduplication, and audit trails)
        entityService.createEntity(entity);

        log.info("Successfully ingested entity: {} for template: {}", entity.identifier(),
            entity.templateIdentifier());

      } catch (Exception e) {
        log.error("Failed to ingest entity for mapping: {} and template: {}", mapping.identifier(),
            mapping.entityTemplateIdentifier(), e);
        throw e; // Fail-fast: propagate exception to caller
      }
    });

    log.info("Completed ingestion for webhook connector: {}",
        webhookConnectorConfiguration.identifier());
  }
}