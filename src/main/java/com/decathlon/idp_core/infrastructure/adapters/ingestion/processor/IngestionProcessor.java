<<<<<<<< HEAD:src/main/java/com/decathlon/idp_core/infrastructure/adapters/ingestion/processors/IngestionProcessor.java
package com.decathlon.idp_core.infrastructure.adapters.ingestion.processors;
========
package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;
>>>>>>>> ca258db (feat(core): init camel ingestion route):src/main/java/com/decathlon/idp_core/infrastructure/adapters/ingestion/processor/IngestionProcessor.java

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
<<<<<<<< HEAD:src/main/java/com/decathlon/idp_core/infrastructure/adapters/ingestion/processors/IngestionProcessor.java
import com.decathlon.idp_core.domain.model.entity_mapping.MappingAction;
========
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
>>>>>>>> ca258db (feat(core): init camel ingestion route):src/main/java/com/decathlon/idp_core/infrastructure/adapters/ingestion/processor/IngestionProcessor.java
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

<<<<<<<< HEAD:src/main/java/com/decathlon/idp_core/infrastructure/adapters/ingestion/processors/IngestionProcessor.java
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

        switch (mapping.action()) {
          case MappingAction.UPSERT :
            log.debug("Call entity service upsert");
            // check if entity exists
            // If not create entity
            // If exists patch entity
            break;
          case MappingAction.UPSERT_PROPERTIES :
            // check if entity exists
            // If not create entity only with the properties information from mapped entity
            // If exists update only the properties information from mmaped entity
            log.debug("Call entity service upsert entity properties");
            break;
          case MappingAction.UPSERT_RELATIONS :
            // check if entity exists
            // If not create entity only with the relations information from mmaped entity
            // If exists update only the relations information from mmaped entity
            log.debug("Call entity service upsert entity relations");
            break;
          case MappingAction.DELETE :
            entityService.deleteEntity(entity.templateIdentifier(), entity.identifier());
            log.debug("Call entity service to delete ");
            break;
          default :
            log.debug("Not supported action");
            break;
        }

        log.info("Successfully ingested entity: {} for template: {}", entity.identifier(),
            entity.templateIdentifier());

      } catch (Exception e) {
        log.error("Failed to ingest entity for mapping: {} and template: {}", mapping.identifier(),
            mapping.entityTemplateIdentifier(), e);
        throw e; // Fail-fast: propagate exception to caller
      }
    });
========
    webhookConnectorConfiguration.mappings().forEach(mapping -> applyMapping(payload, mapping));
>>>>>>>> ca258db (feat(core): init camel ingestion route):src/main/java/com/decathlon/idp_core/infrastructure/adapters/ingestion/processor/IngestionProcessor.java

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