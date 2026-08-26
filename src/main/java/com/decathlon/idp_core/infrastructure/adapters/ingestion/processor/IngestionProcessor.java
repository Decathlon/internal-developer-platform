package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
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
    log.debug("Applying mapping for template: {} with action: {}", mapping.identifier(), mapping.action());
    Entity entity = mappingEngine.mapToEntity(payload, mapping);

    if (entity == null) {
      log.debug("Mapping filter excluded payload for template: {}",
          mapping.entityTemplateIdentifier());
      return;
    }

    switch (mapping.action()) {
      case UPDATE_ENTITY -> handleUpdate(entity);
      case UPDATE_PROPERTIES -> handleUpdateProperties(entity);
      case UPDATE_RELATIONS -> handleUpdateRelations(entity);
      case DELETE_ENTITY -> handleDelete(entity);
      case null, default -> log.warn("Unsupported or null mapping action: {}", mapping.action());
    }

    log.info("Successfully processed action {} for entity: {} under template: {}",
        mapping.action(), entity.identifier(), entity.templateIdentifier());
  }

  /// Handles the Update action for an entity.
  ///
  /// If the entity exists, it is patched with the new data. If the entity does
  /// not
  /// exist, it is created.
  ///
  /// @param entity the entity to Update
  private void handleUpdate(Entity entity) {
    if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
      log.debug("Patching entity {} for template: {}", entity.identifier(),
          entity.templateIdentifier());
      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(), entity);
    } else {
      log.debug("Creating entity {} for template: {}", entity.identifier(),
          entity.templateIdentifier());
      entityService.createEntity(entity);
    }
  }

  /// Handles the Update properties action for an entity.
  ///
  /// If the entity exists, only its properties are patched. If the entity does
  /// not
  /// exist, it is created with only its properties.
  ///
  /// @param entity the entity to Update properties for
  private void handleUpdateProperties(Entity entity) {
    Entity propertiesOnlyEntity = new Entity(entity.id(), entity.templateIdentifier(),
        entity.name(), entity.identifier(), entity.properties(), List.of());

    if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
          propertiesOnlyEntity);
    } else {
      entityService.createEntity(propertiesOnlyEntity);
    }
  }

  /// Handles the Update relations action for an entity. If the entity exists,
  /// only its relations are patched. The entity must exists in order to patch the
  /// relations
  ///
  /// @param entity the entity to Update relations for
  /// @throws EntityNotFoundException
  private void handleUpdateRelations(Entity entity) {

    if (!entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
      throw new EntityNotFoundException(entity.templateIdentifier(), entity.identifier());
    }
    // Strip properties before invoking entity service
    Entity relationsOnlyEntity = new Entity(null, entity.templateIdentifier(), null,
        entity.identifier(), List.of(), entity.relations());

    entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
        relationsOnlyEntity);

  }

  /// Handles the delete action for an entity.
  ///
  /// @param entity the entity to delete
  private void handleDelete(Entity entity) {
    entityService.deleteEntity(entity.templateIdentifier(), entity.identifier());
    log.debug("Deleted entity: {} for template: {}", entity.identifier(),
        entity.templateIdentifier());
  }
}
