package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityPatch;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionMetricConstants;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class IngestionProcessor {

  private static final String SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER = "idp.webhook.identifier";

  private final MappingEnginePort mappingEngine;
  private final EntityService entityService;
  private final MeterRegistry meterRegistry;

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
    String connectorIdentifier = webhookConnectorConfiguration.identifier();
    Span.current().setAttribute(SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER, connectorIdentifier);

    log.info("Starting ingestion for webhook connector: {}", connectorIdentifier);
    webhookConnectorConfiguration.mappings()
        .forEach(mapping -> applyMapping(payload, mapping, webhookConnectorConfiguration));
    log.info("Completed ingestion for webhook connector: {}", connectorIdentifier);
  }

  /// Applies a single mapping to the payload and persists the resulting entity.
  ///
  /// Skips silently if the mapping filter excludes the payload (returns null).
  ///
  /// @param payload the raw JSON payload from the webhook
  /// @param mapping the mapping definition to apply
  private void applyMapping(String payload, EntityDynamicMapping mapping, WebhookConnector webhookConnectorConfiguration) {
    String connectorIdentifier = webhookConnectorConfiguration.identifier();
    String mappingId = mapping.identifier();
    String action = mapping.action() == null ? "unknown" : mapping.action().name();
    long startTimeNanos = System.nanoTime();

    meterRegistry.counter(IngestionMetricConstants.MAPPING_APPLIED_TOTAL, "connector", connectorIdentifier, "mapping_id", mappingId, "action", action).increment();

    try {
      Entity entity = mappingEngine.mapToEntity(payload, mapping);

      if (entity == null) {
        meterRegistry.counter(IngestionMetricConstants.MAPPING_FILTERED_TOTAL, "connector", connectorIdentifier,
            "mapping_id", mappingId, "reason", "filter_rejected").increment();
        meterRegistry.counter(IngestionMetricConstants.MAPPING_RESULT, "connector", connectorIdentifier,
            "mapping_id", mappingId, "result", "FILTERED_OUT").increment();
        return;
      }

      meterRegistry.counter(IngestionMetricConstants.MAPPING_RESULT, "connector", connectorIdentifier,
          "mapping_id", mappingId, "result", "MATCHED").increment();

      switch (mapping.action()) {
        case UPDATE_ENTITY -> handleUpdate(entity, connectorIdentifier);
        case UPDATE_PROPERTIES -> handleUpdateProperties(entity, connectorIdentifier);
        case UPDATE_RELATIONS -> handleUpdateRelations(entity, connectorIdentifier);
        case DELETE_ENTITY -> handleDelete(entity, connectorIdentifier);
        case null, default -> log.warn("Unsupported or null mapping action: {}", mapping.action());
      }

      log.info("Successfully processed action {} for entity: {} under template: {}",
          mapping.action(), entity.identifier(), entity.templateIdentifier());
    } catch (RuntimeException exception) {
      meterRegistry.counter(IngestionMetricConstants.MAPPING_ERRORS_TOTAL, "connector", connectorIdentifier,
          "mapping_id", mappingId, "error_type", exception.getClass().getSimpleName())
          .increment();
      meterRegistry.counter(IngestionMetricConstants.MAPPING_RESULT, "connector", connectorIdentifier,
          "mapping_id", mappingId, "result", "ERROR").increment();
      meterRegistry.counter(IngestionMetricConstants.FUNCTIONAL_VALIDATION_FAILED_TOTAL, "connector", connectorIdentifier,
          "mapping_id", mappingId, "reason", exception.getClass().getSimpleName()).increment();
      recordEntityFailureMetrics(connectorIdentifier, exception);
      throw exception;
    } finally {
      long durationMs = (System.nanoTime() - startTimeNanos) / 1_000_000L;
      meterRegistry.summary(IngestionMetricConstants.MAPPING_LATENCY_MS, "connector", connectorIdentifier,
          "mapping_id", mappingId).record(durationMs);
    }
  }

  /// Handles the Update action for an entity.
  ///
  /// If the entity exists, it is patched with the new data. If the entity does
  /// not exist, it is created.
  ///
  /// @param entity the entity to Update
  private void handleUpdate(Entity entity, String connectorIdentifier) {
    try {
      if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        log.debug("Patching entity {} for template: {}", entity.identifier(),
            entity.templateIdentifier());
        entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
            EntityPatch.fromEntity(entity));
        meterRegistry.counter(IngestionMetricConstants.ENTITY_PATCH_TOTAL, "template",
            entity.templateIdentifier(), "connector", connectorIdentifier, "action",
            "UPDATE_ENTITY").increment();
        meterRegistry.counter(IngestionMetricConstants.ENTITY_UPDATED_TOTAL, "template",
            entity.templateIdentifier(), "connector", connectorIdentifier).increment();
      } else {
        log.debug("Creating entity {} for template: {}", entity.identifier(),
            entity.templateIdentifier());
        entityService.createEntity(entity);
        meterRegistry.counter(IngestionMetricConstants.ENTITY_CREATED_TOTAL, "template",
            entity.templateIdentifier(), "connector", connectorIdentifier).increment();
      }
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  /// Handles the Update properties action for an entity.
  ///
  /// If the entity exists, only its properties are patched. If the entity does
  /// not exist, it is created with only its properties.
  ///
  /// @param entity the entity to Update properties for
  private void handleUpdateProperties(Entity entity, String connectorIdentifier) {
    EntityPatch propertiesPatch = new EntityPatch(entity.name(), entity.properties(), List.of());

    try {
      if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
            propertiesPatch);
        meterRegistry.counter(IngestionMetricConstants.ENTITY_PATCH_TOTAL, "template",
            entity.templateIdentifier(), "connector", connectorIdentifier, "action",
            "UPDATE_PROPERTIES").increment();
        meterRegistry.counter(IngestionMetricConstants.ENTITY_UPDATED_TOTAL, "template",
            entity.templateIdentifier(), "connector", connectorIdentifier).increment();
      } else {
        entityService.createEntity(new Entity(null, entity.templateIdentifier(), entity.name(),
            entity.identifier(), entity.properties(), List.of()));
        meterRegistry.counter(IngestionMetricConstants.ENTITY_CREATED_TOTAL, "template",
            entity.templateIdentifier(), "connector", connectorIdentifier).increment();
      }
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  /// Handles the Update relations action for an entity. If the entity exists,
  /// only its relations are patched. The entity must exist in order to patch the
  /// relations.
  ///
  /// @param entity the entity to update relations for
  /// @throws EntityNotFoundException when the entity does not exist
  private void handleUpdateRelations(Entity entity, String connectorIdentifier) {
    try {
      if (!entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        throw new EntityNotFoundException(entity.templateIdentifier(), entity.identifier());
      }
      EntityPatch relationsPatch = new EntityPatch(null, List.of(), entity.relations());

      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(), relationsPatch);
      meterRegistry.counter(IngestionMetricConstants.ENTITY_PATCH_TOTAL, "template",
          entity.templateIdentifier(), "connector", connectorIdentifier, "action",
          "UPDATE_RELATIONS").increment();
      meterRegistry.counter(IngestionMetricConstants.ENTITY_UPDATED_TOTAL, "template",
          entity.templateIdentifier(), "connector", connectorIdentifier).increment();
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  /// Handles the delete action for an entity.
  ///
  /// @param entity the entity to delete
  private void handleDelete(Entity entity, String connectorIdentifier) {
    try {
      entityService.deleteEntity(entity.templateIdentifier(), entity.identifier());
      meterRegistry.counter(IngestionMetricConstants.ENTITY_DELETED_TOTAL, "template",
          entity.templateIdentifier(), "connector", connectorIdentifier).increment();
      log.debug("Deleted entity: {} for template: {}", entity.identifier(),
          entity.templateIdentifier());
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  private void recordEntityFailureMetrics(String connectorIdentifier, RuntimeException exception) {
    recordEntityFailureMetrics(connectorIdentifier, exception, "unknown");
  }

  private void recordEntityFailureMetrics(String connectorIdentifier, RuntimeException exception,
      String templateIdentifier) {
    meterRegistry.counter(IngestionMetricConstants.ENTITY_VALIDATION_ERRORS_TOTAL, "template",
        templateIdentifier, "connector", connectorIdentifier, "reason",
        exception.getClass().getSimpleName()).increment();
    meterRegistry.counter(IngestionMetricConstants.FUNCTIONAL_VALIDATION_FAILED_TOTAL, "connector",
        connectorIdentifier, "template", templateIdentifier, "reason",
        exception.getClass().getSimpleName()).increment();

    if (exception instanceof EntityNotFoundException) {
      meterRegistry.counter(IngestionMetricConstants.ENTITY_NOT_FOUND_TOTAL, "template",
          templateIdentifier, "connector", connectorIdentifier, "action", "ingestion").increment();
    }
    if (exception instanceof EntityAlreadyExistsException) {
      meterRegistry.counter(IngestionMetricConstants.ENTITY_CONFLICT_TOTAL, "template",
          templateIdentifier, "connector", connectorIdentifier, "reason", "already_exists")
          .increment();
    }
    if (exception instanceof EntityValidationException) {
      meterRegistry.counter(IngestionMetricConstants.ENTITY_VALIDATION_ERRORS_TOTAL, "template",
          templateIdentifier, "connector", connectorIdentifier, "reason",
          exception.getClass().getSimpleName()).increment();
    }
  }
}
