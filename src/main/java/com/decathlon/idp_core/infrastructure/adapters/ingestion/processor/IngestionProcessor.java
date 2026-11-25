package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.*;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityPatch;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.PrincipalProvisioningProperties;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics.IngestionObservabilitySupport;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IngestionProcessor {

  private static final Set<String> JIT_PROTECTED_PRINCIPAL_PROPERTIES = Set.of("kind", "is_admin",
      "is_idp_user");
  private static final Set<String> LINK_ONLY_PROTECTED_PRINCIPAL_PROPERTIES = Set.of("is_admin",
      "is_idp_user");
  private final PrincipalProvisioningProperties provisioningProperties;
  private static final String SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER = "idp.webhook.identifier";

  private final MappingEnginePort mappingEngine;
  private final EntityService entityService;

  private final LongCounter mappingAppliedCounter;
  private final LongCounter mappingFilteredCounter;
  private final LongCounter mappingResultCounter;
  private final LongCounter mappingErrorsCounter;
  private final DoubleHistogram mappingLatencyHistogram;

  private final LongCounter entityCreatedCounter;
  private final LongCounter entityUpdatedCounter;
  private final LongCounter entityDeletedCounter;
  private final LongCounter entityPatchCounter;
  private final LongCounter entityValidationErrorsCounter;
  private final LongCounter functionalValidationFailedCounter;
  private final LongCounter entityNotFoundCounter;
  private final LongCounter entityConflictCounter;

  public IngestionProcessor(PrincipalProvisioningProperties provisioningProperties,
      MappingEnginePort mappingEngine, EntityService entityService,
      @Qualifier("ingestionMeter") Meter meter) {
    this.provisioningProperties = provisioningProperties;
    this.mappingEngine = mappingEngine;
    this.entityService = entityService;

    this.mappingAppliedCounter = meter.counterBuilder(MAPPING_APPLIED_TOTAL)
        .setDescription("Mapping execution attempts").build();
    this.mappingFilteredCounter = meter.counterBuilder(MAPPING_FILTERED_TOTAL)
        .setDescription("Mapping results rejected by filter stage").build();
    this.mappingResultCounter = meter.counterBuilder(MAPPING_RESULT)
        .setDescription("Terminal mapping outcome").build();
    this.mappingErrorsCounter = meter.counterBuilder(MAPPING_ERRORS_TOTAL)
        .setDescription("Mapping exceptions and runtime failures").build();
    this.mappingLatencyHistogram = meter.histogramBuilder(MAPPING_LATENCY_MS)
        .setDescription("Mapping duration in milliseconds").setUnit("ms").build();

    this.entityCreatedCounter = meter.counterBuilder(ENTITY_CREATED_TOTAL)
        .setDescription("Number of entities created during ingestion").build();
    this.entityUpdatedCounter = meter.counterBuilder(ENTITY_UPDATED_TOTAL)
        .setDescription("Number of entities updated during ingestion").build();
    this.entityDeletedCounter = meter.counterBuilder(ENTITY_DELETED_TOTAL)
        .setDescription("Number of entities deleted during ingestion").build();
    this.entityPatchCounter = meter.counterBuilder(ENTITY_PATCH_TOTAL)
        .setDescription("Number of patch operations applied").build();
    this.entityValidationErrorsCounter = meter.counterBuilder(ENTITY_VALIDATION_ERRORS_TOTAL)
        .setDescription("Entity validation and business-rule failures").build();
    this.functionalValidationFailedCounter = meter
        .counterBuilder(FUNCTIONAL_VALIDATION_FAILED_TOTAL)
        .setDescription("Functional validation failures during ingestion").build();
    this.entityNotFoundCounter = meter.counterBuilder(ENTITY_NOT_FOUND_TOTAL)
        .setDescription("Missing entity operations during ingestion").build();
    this.entityConflictCounter = meter.counterBuilder(ENTITY_CONFLICT_TOTAL)
        .setDescription("Conflict scenarios during ingestion").build();
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

    mappingAppliedCounter.add(1, Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY,
        mappingId, ACTION_KEY, action));

    try {
      Entity mappedEntity = mappingEngine.mapToEntity(payload, mapping);

      if (mappedEntity == null) {
        mappingFilteredCounter.add(1,
            Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY, mappingId,
                REASON_KEY, "filter_rejected"));
        mappingResultCounter.add(1,
            Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY, mappingId,
                RESULT_KEY, "FILTERED_OUT"));
        return;
      }
      Entity entity = protectPrincipalProperties(mappedEntity);
      switch (mapping.action()) {
        case UPDATE_ENTITY -> handleUpdate(entity, connectorIdentifier, action);
        case UPDATE_PROPERTIES -> handleUpdateProperties(entity, connectorIdentifier, action);
        case UPDATE_RELATIONS -> handleUpdateRelations(entity, connectorIdentifier, action);
        case DELETE_ENTITY -> handleDelete(entity, connectorIdentifier, action);
        case null, default -> throw new IllegalStateException(
            "Unsupported or null mapping action: " + mapping.action());
      }
      log.info("Successfully processed action {} for entity: {} under template: {}",
          mapping.action(), entity.identifier(), entity.templateIdentifier());

      mappingResultCounter.add(1,
          Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY, mappingId,
              RESULT_KEY, "MATCHED"));

    } catch (RuntimeException exception) {
      mappingErrorsCounter.add(1,
          Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY, mappingId,
              ERROR_TYPE_KEY, exception.getClass().getSimpleName()));
      mappingResultCounter.add(1,
          Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY, mappingId,
              RESULT_KEY, "ERROR"));
      throw exception;
    } finally {
      double durationMs = (System.nanoTime() - startTimeNanos) / 1_000_000.0d;
      mappingLatencyHistogram.record(durationMs,
          Attributes.of(CONNECTOR_KEY, connectorIdentifier, MAPPING_ID_KEY, mappingId));
    }
  }

  private void handleUpdate(Entity entity, String connectorIdentifier, String action) {
    Entity sanitizedEntity = protectPrincipalProperties(entity);

    try {
      if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        log.debug("Patching entity {} for template: {}", entity.identifier(),
            entity.templateIdentifier());
        entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
            EntityPatch.fromEntity(sanitizedEntity));
        entityPatchCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
            CONNECTOR_KEY, connectorIdentifier, ACTION_KEY, "UPDATE_ENTITY"));
        entityUpdatedCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
            CONNECTOR_KEY, connectorIdentifier));
      } else {
        log.debug("Creating entity {} for template: {}", entity.identifier(),
            entity.templateIdentifier());
        entityService.createEntity(sanitizedEntity);
        entityCreatedCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
            CONNECTOR_KEY, connectorIdentifier));
      }
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier(),
          action);
      throw exception;
    }
  }

  private void handleUpdateProperties(Entity entity, String connectorIdentifier, String action) {
    Entity sanitizedEntity = protectPrincipalProperties(entity);
    EntityPatch propertiesPatch = new EntityPatch(entity.name(), sanitizedEntity.properties(),
        List.of());

    try {
      if (entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        entityService.patchEntity(entity.templateIdentifier(), entity.identifier(),
            propertiesPatch);
        entityPatchCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
            CONNECTOR_KEY, connectorIdentifier, ACTION_KEY, "UPDATE_PROPERTIES"));
        entityUpdatedCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
            CONNECTOR_KEY, connectorIdentifier));
      } else {
        entityService.createEntity(new Entity(null, entity.templateIdentifier(), entity.name(),
            entity.identifier(), sanitizedEntity.properties(), List.of()));
        entityCreatedCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
            CONNECTOR_KEY, connectorIdentifier));
      }
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier(),
          action);
      throw exception;
    }
  }

  private Entity protectPrincipalProperties(Entity entity) {
    if (!AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER.equals(entity.templateIdentifier())) {
      return entity;
    }
    Set<String> protectedProperties = switch (provisioningProperties.mode()) {
      case JIT -> JIT_PROTECTED_PRINCIPAL_PROPERTIES;
      case LINK_ONLY -> LINK_ONLY_PROTECTED_PRINCIPAL_PROPERTIES;
    };
    List<Property> properties = entity.properties().stream()
        .filter(property -> !protectedProperties.contains(property.name())).toList();
    return new Entity(entity.id(), entity.templateIdentifier(), entity.name(), entity.identifier(),
        properties, entity.relations());
  }

  private void handleUpdateRelations(Entity entity, String connectorIdentifier, String action) {
    try {
      if (!entityService.entityExists(entity.templateIdentifier(), entity.identifier())) {
        throw new EntityNotFoundException(entity.templateIdentifier(), entity.identifier());
      }
      EntityPatch relationsPatch = new EntityPatch(null, List.of(), entity.relations());

      entityService.patchEntity(entity.templateIdentifier(), entity.identifier(), relationsPatch);
      entityPatchCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
          CONNECTOR_KEY, connectorIdentifier, ACTION_KEY, "UPDATE_RELATIONS"));
      entityUpdatedCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
          CONNECTOR_KEY, connectorIdentifier));
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier(),
          action);
      throw exception;
    }
  }

  private void handleDelete(Entity entity, String connectorIdentifier, String action) {
    try {
      entityService.deleteEntity(entity.templateIdentifier(), entity.identifier());
      entityDeletedCounter.add(1, Attributes.of(TEMPLATE_KEY, entity.templateIdentifier(),
          CONNECTOR_KEY, connectorIdentifier));
      log.debug("Deleted entity: {} for template: {}", entity.identifier(),
          entity.templateIdentifier());
    } catch (RuntimeException exception) {
      recordEntityFailureMetrics(connectorIdentifier, exception, entity.templateIdentifier(),
          action);
      throw exception;
    }
  }

  private void recordEntityFailureMetrics(String connectorIdentifier, RuntimeException exception,
      String templateIdentifier, String action) {
    functionalValidationFailedCounter.add(1, Attributes.of(CONNECTOR_KEY, connectorIdentifier,
        TEMPLATE_KEY, templateIdentifier, REASON_KEY, exception.getClass().getSimpleName()));

    if (exception instanceof EntityNotFoundException) {
      entityNotFoundCounter.add(1, Attributes.of(TEMPLATE_KEY, templateIdentifier, CONNECTOR_KEY,
          connectorIdentifier, ACTION_KEY, action));
    }
    if (exception instanceof EntityAlreadyExistsException) {
      entityConflictCounter.add(1, Attributes.of(TEMPLATE_KEY, templateIdentifier, CONNECTOR_KEY,
          connectorIdentifier, REASON_KEY, "already_exists"));
    }
    if (exception instanceof EntityValidationException) {
      entityValidationErrorsCounter.add(1, Attributes.of(TEMPLATE_KEY, templateIdentifier,
          CONNECTOR_KEY, connectorIdentifier, REASON_KEY, exception.getClass().getSimpleName()));
    }
  }
}
