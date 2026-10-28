package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.*;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityPatch;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics.IngestionObservabilitySupport;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IngestionProcessor {

  private final MappingEnginePort mappingEngine;
  private final EntityService entityService;
  private final Meter meter;

  public IngestionProcessor(MappingEnginePort mappingEngine, EntityService entityService,
      @Qualifier("ingestionMeter") Meter meter) {
    this.mappingEngine = mappingEngine;
    this.entityService = entityService;
    this.meter = meter;
  }

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
    Span currentSpan = Span.current();
    IngestionObservabilitySupport.setSpanAttribute(currentSpan, SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER,
        connectorIdentifier);
    log.info("span  id: {}, trace_id: {}", currentSpan.getSpanContext().getSpanId(),
        currentSpan.getSpanContext().getTraceId());// todo todelete
    log.info("Starting ingestion for webhook connector: {}", connectorIdentifier);
    webhookConnectorConfiguration.mappings()
        .forEach(mapping -> applyMapping(payload, mapping, webhookConnectorConfiguration));
    log.info("Completed ingestion for webhook connector: {}", connectorIdentifier);
  }

  private void applyMapping(String payload, EntityDynamicMapping mapping,
      WebhookConnector webhookConnectorConfiguration) {
    String connectorIdentifier = webhookConnectorConfiguration.identifier();
    String mappingId = mapping.identifier();
    String action = mapping.action() == null ? "unknown" : mapping.action().name();
    long startTimeNanos = System.nanoTime();

    incrementCounter(MAPPING_APPLIED_TOTAL, ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId, ATTRIBUTE_ACTION, action);

    try {
      Entity entity = mappingEngine.mapToEntity(payload, mapping);

      if (entity == null) {
        incrementCounter(MAPPING_FILTERED_TOTAL,
            ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId, ATTRIBUTE_REASON,
            "filter_rejected");
        incrementCounter(MAPPING_RESULT,
            ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId, ATTRIBUTE_RESULT,
            "FILTERED_OUT");
        return;
      }

      incrementCounter(MAPPING_RESULT,
          ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId, ATTRIBUTE_RESULT,
          "MATCHED");

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
      incrementCounter(MAPPING_ERRORS_TOTAL,
          ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId,
          "error_type", exception.getClass().getSimpleName());
      incrementCounter(MAPPING_RESULT,
          ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId, ATTRIBUTE_RESULT, "ERROR");
      incrementCounter(IngestionObservabilityConstants.FUNCTIONAL_VALIDATION_FAILED_TOTAL,
          ATTRIBUTE_CONNECTOR, connectorIdentifier, "mapping_id", mappingId,
          ATTRIBUTE_REASON, exception.getClass().getSimpleName());
      recordEntityFailureMetrics(connectorIdentifier, exception);
      throw exception;
    } finally {
      long durationMs = (System.nanoTime() - startTimeNanos) / 1_000_000L;
      recordHistogram(IngestionObservabilityConstants.MAPPING_LATENCY_MS, durationMs,
          ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_MAPPING_ID, mappingId);
    }
  }

  private void handleUpdate(Entity entity, String connectorIdentifier) {
    try {
      if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        log.debug("Patching entity {} for template: {}", entity.identifier(),
            entity.templateIdentifier());
        entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
            EntityPatch.fromEntity(entity));
        incrementCounter(IngestionObservabilityConstants.ENTITY_PATCH_TOTAL, ATTRIBUTE_TEMPLATE,
            entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_ACTION,
            "UPDATE_ENTITY");
        incrementCounter(IngestionObservabilityConstants.ENTITY_UPDATED_TOTAL, ATTRIBUTE_TEMPLATE,
            entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier);
      } else {
        log.debug("Creating entity {} for template: {}", entity.identifier(),
            entity.templateIdentifier());
        entityService.createEntity(entity);
        incrementCounter(IngestionObservabilityConstants.ENTITY_CREATED_TOTAL, ATTRIBUTE_TEMPLATE,
            entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier);
      }
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  private void handleUpdateProperties(Entity entity, String connectorIdentifier) {
    EntityPatch propertiesPatch = new EntityPatch(entity.name(), entity.properties(), List.of());

    try {
      if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
            propertiesPatch);
        incrementCounter(IngestionObservabilityConstants.ENTITY_PATCH_TOTAL, ATTRIBUTE_TEMPLATE,
            entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_ACTION,
            "UPDATE_PROPERTIES");
        incrementCounter(IngestionObservabilityConstants.ENTITY_UPDATED_TOTAL, ATTRIBUTE_TEMPLATE,
            entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier);
      } else {
        entityService.createEntity(new Entity(null, entity.templateIdentifier(), entity.name(),
            entity.identifier(), entity.properties(), List.of()));
        incrementCounter(IngestionObservabilityConstants.ENTITY_CREATED_TOTAL, ATTRIBUTE_TEMPLATE,
            entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier);
      }
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  private void handleUpdateRelations(Entity entity, String connectorIdentifier) {
    try {
      if (!entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        throw new EntityNotFoundException(entity.templateIdentifier(), entity.identifier());
      }
      EntityPatch relationsPatch = new EntityPatch(null, List.of(), entity.relations());

      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(), relationsPatch);
      incrementCounter(IngestionObservabilityConstants.ENTITY_PATCH_TOTAL, ATTRIBUTE_TEMPLATE,
          entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_ACTION,
          "UPDATE_RELATIONS");
      incrementCounter(IngestionObservabilityConstants.ENTITY_UPDATED_TOTAL, ATTRIBUTE_TEMPLATE,
          entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier);
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier());
      throw exception;
    }
  }

  private void handleDelete(Entity entity, String connectorIdentifier) {
    try {
      entityService.deleteEntity(entity.templateIdentifier(), entity.identifier());
      incrementCounter(IngestionObservabilityConstants.ENTITY_DELETED_TOTAL, ATTRIBUTE_TEMPLATE,
          entity.templateIdentifier(), ATTRIBUTE_CONNECTOR, connectorIdentifier);
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
    incrementCounter(IngestionObservabilityConstants.ENTITY_VALIDATION_ERRORS_TOTAL,
        ATTRIBUTE_TEMPLATE, templateIdentifier, ATTRIBUTE_CONNECTOR, connectorIdentifier,
        ATTRIBUTE_REASON, exception.getClass().getSimpleName());
    incrementCounter(IngestionObservabilityConstants.FUNCTIONAL_VALIDATION_FAILED_TOTAL,
        ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_TEMPLATE, templateIdentifier,
        ATTRIBUTE_REASON, exception.getClass().getSimpleName());

    if (exception instanceof EntityNotFoundException) {
      incrementCounter(IngestionObservabilityConstants.ENTITY_NOT_FOUND_TOTAL, ATTRIBUTE_TEMPLATE,
          templateIdentifier, ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_ACTION,
          "ingestion");
    }
    if (exception instanceof EntityAlreadyExistsException) {
      incrementCounter(IngestionObservabilityConstants.ENTITY_CONFLICT_TOTAL, ATTRIBUTE_TEMPLATE,
          templateIdentifier, ATTRIBUTE_CONNECTOR, connectorIdentifier, ATTRIBUTE_REASON,
          "already_exists");
    }
    if (exception instanceof EntityValidationException) {
      incrementCounter(IngestionObservabilityConstants.ENTITY_VALIDATION_ERRORS_TOTAL,
          ATTRIBUTE_TEMPLATE, templateIdentifier, ATTRIBUTE_CONNECTOR, connectorIdentifier,
          ATTRIBUTE_REASON, exception.getClass().getSimpleName());
    }
  }

  private void incrementCounter(String metricName, String... attributes) {
    IngestionObservabilitySupport.incrementCounter(meter, metricName, attributes);
  }

  private void recordHistogram(String metricName, long value, String... attributes) {
    IngestionObservabilitySupport.recordHistogram(meter, metricName, value, attributes);
  }
}
