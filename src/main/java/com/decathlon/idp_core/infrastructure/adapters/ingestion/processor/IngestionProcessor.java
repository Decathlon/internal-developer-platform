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
    Entity entity = mappingEngine.mapToEntity(payload, mapping);

    if (entity == null) {
      log.debug("Mapping filter excluded payload for template: {}",
              mapping.entityTemplateIdentifier());
      return;
    }

    boolean exists = entityService.entityExists(entity.templateIdentifier(), entity.identifier());

    switch (mapping.action()) {
      case UPSERT -> handleUpsert(entity, exists);
      case UPSERT_PROPERTIES -> handleUpsertProperties(entity, exists);
      case UPSERT_RELATIONS -> handleUpsertRelations(entity, exists);
      case PATCH_PROPERTIES -> handlePatchProperties(entity, exists);
      case PATCH_RELATIONS -> handlePatchRelations(entity, exists);
      case DELETE -> handleDelete(entity);
      case null, default -> log.warn("Unsupported or null mapping action: {}", mapping.action());
    }

    log.info("Successfully processed action {} for entity: {} under template: {}",
            mapping.action(), entity.identifier(), entity.templateIdentifier());
  }

  /// Handles the upsert action for an entity.
  ///
  /// If the entity exists, it is patched with the new data.
  /// If the entity does not exist, it is created.
  ///
  /// @param entity the entity to upsert
  /// @param exists whether the entity already exists
  private void handleUpsert(Entity entity, boolean exists) {
    if (exists) {
      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(), entity);
    } else {
      entityService.createEntity(entity);
    }
  }

  /// Handles the upsert properties action for an entity.
  ///
  /// If the entity exists, only its properties are patched.
  /// If the entity does not exist, it is created with only its properties.
  ///
  /// @param entity the entity to upsert properties for
  /// @param exists whether the entity already exists
  private void handleUpsertProperties(Entity entity, boolean exists) {
    Entity propertiesOnlyEntity = stripRelations(entity);
    if (exists) {
      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
          propertiesOnlyEntity);
    } else {
      entityService.createEntity(propertiesOnlyEntity);
    }
  }

  /// Handles the upsert relations action for an entity.
  ///
  /// If the entity exists, only its relations are patched.
  /// If the entity does not exist, it is created with only its relations.
  ///
  /// @param entity the entity to upsert relations for
  /// @param exists whether the entity already exists
  private void handleUpsertRelations(Entity entity, boolean exists) {
    Entity relationsOnlyEntity = stripProperties(entity);
    if (exists) {
      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
          relationsOnlyEntity);
    } else {
      entityService.createEntity(relationsOnlyEntity);
    }
  }

  /// Handles the update properties action for an entity.
  ///
  /// If the entity exists, only its relations are patched.
  /// If the entity does not exist, throw an Entity not found exception.
  ///
  /// @param entity the entity to upsert relations for
  /// @param exists whether the entity already exists
  private void handlePatchProperties(Entity entity, boolean exists) {
    requireExistingEntity(entity, exists);
    entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
        stripRelations(entity));
  }

  /// Handles the update relations action for an entity.
  ///
  /// If the entity exists, only its relations are patched.
  /// If the entity does not exist, throw an Entity not found exception.
  ///
  /// @param entity the entity to upsert relations for
  /// @param exists whether the entity already exists
  private void handlePatchRelations(Entity entity, boolean exists) {
    requireExistingEntity(entity, exists);
    entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
        stripProperties(entity));
  }

  /// Handles the delete action for an entity.
  ///
  /// @param entity the entity to delete
  private void handleDelete(Entity entity) {
    entityService.deleteEntity(entity.templateIdentifier(), entity.identifier());
    log.debug("Deleted entity: {} for template: {}", entity.identifier(),
        entity.templateIdentifier());
  }

  private void requireExistingEntity(Entity entity, boolean exists) {
    if (!exists) {
      throw new EntityNotFoundException(entity.templateIdentifier(), entity.identifier());
    }
  }

  private Entity stripRelations(Entity entity) {
    return new Entity(entity.id(), entity.templateIdentifier(), entity.name(), entity.identifier(),
        entity.properties(), List.of());
  }

  private Entity stripProperties(Entity entity) {
    return new Entity(entity.id(), entity.templateIdentifier(), entity.name(), entity.identifier(),
        List.of(), entity.relations());
  }
}
