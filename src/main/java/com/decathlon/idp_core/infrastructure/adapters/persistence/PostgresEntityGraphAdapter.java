package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityGraphJsonProjection;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/// Persistence adapter dedicated to entity relationship graph traversal.
///
/// Separated from [PostgresEntityAdapter] because graph queries use a distinct
/// recursive CTE strategy that has no overlap with standard CRUD operations,
/// following the Interface Segregation Principle.
///
/// **Query strategy:**
/// 1. A recursive CTE discovers reachable entity UUIDs.
/// 2. A projection query returns entity fields plus JSONB-aggregated relations
///    and properties, avoiding entity hydration and N+1 loading.
@Component
public class PostgresEntityGraphAdapter implements EntityGraphRepositoryPort {

  private static final String FIND_ENTITY_IDS_SPAN = "entity_graph.database.find_entity_ids";
  private static final String FIND_GRAPH_DATA_SPAN = "entity_graph.database.find_graph_data";
  private static final String PROJECTION_MAPPING_SPAN = "entity_graph.projection_mapping";

  private final JpaEntityRepository jpaEntityRepository;
  private final ObjectReader propertyReader;
  private final ObjectReader relationReader;
  private final Tracer tracer;

  /// Creates the adapter and prepares readers for the JSONB projection payloads.
  ///
  /// @param jpaEntityRepository repository used to discover and project graph
  /// data
  /// @param objectMapper application-configured mapper used for JSON
  /// projection
  /// parsing
  /// @param openTelemetry application OpenTelemetry instance used to create
  /// child spans
  public PostgresEntityGraphAdapter(JpaEntityRepository jpaEntityRepository,
      ObjectMapper objectMapper, OpenTelemetry openTelemetry) {
    this.jpaEntityRepository = jpaEntityRepository;
    this.propertyReader = objectMapper.readerFor(new TypeReference<List<PropertyProjection>>() {
    });
    this.relationReader = objectMapper.readerFor(new TypeReference<List<RelationProjection>>() {
    });
    this.tracer = openTelemetry.getTracer(PostgresEntityGraphAdapter.class.getName());
  }

  /// Fetches a depth-limited entity relationship graph for one or more root
  /// entities.
  ///
  /// **Purpose:** Implements the graph repository contract using a recursive
  /// CTE to discover reachable UUIDs, followed by one JSON projection query and
  /// conversion of those projections to domain entities.
  ///
  /// **Query strategy:** The first query discovers IDs using the requested
  /// traversal mode and depth. The second query projects those entities and
  /// aggregates relations and selected properties as JSONB, without loading JPA
  /// entity collections.
  ///
  /// @param rootIds UUIDs of entities to use as traversal
  /// roots
  /// @param depth maximum number of relationship steps to
  /// traverse
  /// @param includeProperties whether to include properties for every
  /// discovered
  /// entity
  /// @param includeRootEntityProperties when true and `includeProperties` is
  /// false, include properties only for the
  /// requested roots
  /// @param mode traversal direction and flow semantics
  /// @return map of discovered entities keyed by UUID; empty when roots are
  /// empty,
  /// the mode is null, the depth is less than one, or no entities
  /// are discovered
  @Override
  @Transactional(readOnly = true)
  @WithSpan("PostgresEntityGraphAdapter.findEntityGraph")
  public Map<UUID, Entity> findEntityGraph(Collection<UUID> rootIds, int depth,
      boolean includeProperties, boolean includeRootEntityProperties,
      EntityGraphTraversalMode mode) {
    // Restore null and empty-input guard before executing CTE query or calling
    // mode.name()
    if (rootIds == null || rootIds.isEmpty() || mode == null || depth < 1) {
      return Map.of();
    }
    List<UUID> discoveredIds = traceOperation(FIND_ENTITY_IDS_SPAN,
        () -> jpaEntityRepository.findEntityIdsInGraph(rootIds, depth, mode.name()));
    if (discoveredIds.isEmpty()) {
      return Map.of();
    }

    List<EntityGraphJsonProjection> projections = traceOperation(FIND_GRAPH_DATA_SPAN,
        () -> jpaEntityRepository.findEntityGraphDataByIds(discoveredIds, rootIds,
            includeProperties, includeRootEntityProperties));
    return traceOperation(PROJECTION_MAPPING_SPAN, () -> {
      Map<UUID, Entity> entities = HashMap.newHashMap(projections.size());

      for (EntityGraphJsonProjection projection : projections) {
        Entity entity = mapProjectionToDomain(projection);
        if (entities.putIfAbsent(entity.id(), entity) != null) {
          throw new IllegalStateException(
              "Duplicate entity ID in graph projection: " + entity.id());
        }
      }
      return entities;
    });
  }

  private <T> T traceOperation(String spanName, Supplier<T> operation) {
    Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.INTERNAL).startSpan();
    try (Scope scope = span.makeCurrent()) {
      return operation.get();
    } catch (RuntimeException | Error exception) {
      span.recordException(exception);
      span.setStatus(StatusCode.ERROR);
      throw exception;
    } finally {
      span.end();
    }
  }

  // Suppress "Use a stream instead of this loop" rule warning
  // This path maps JSON projections into entities,
  // and explicit loops avoid building stream pipelines and intermediate
  // collections
  @SuppressWarnings("java:S9391")
  private Entity mapProjectionToDomain(EntityGraphJsonProjection projection) {
    try {
      List<PropertyProjection> propertyProjections = propertyReader
          .readValue(jsonOrEmptyArray(projection.getPropertiesJson()));
      List<RelationProjection> relationProjections = relationReader
          .readValue(jsonOrEmptyArray(projection.getRelationsJson()));

      // 1. Map and sort properties by name
      List<Property> properties = new java.util.ArrayList<>(propertyProjections.size());
      for (PropertyProjection property : propertyProjections) {
        properties.add(new Property(property.id(), property.name(), property.value()));
      }
      properties.sort(java.util.Comparator.comparing(Property::name,
          java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

      // 2. Map and sort relations by name, and target identifiers lexically
      List<Relation> relations = new java.util.ArrayList<>(relationProjections.size());
      for (RelationProjection relation : relationProjections) {
        List<String> targetIdentifiers = new java.util.ArrayList<>(
            relation.targetEntities().size());
        for (TargetProjection target : relation.targetEntities()) {
          if (Objects.nonNull(target.targetEntityIdentifier())) {
            targetIdentifiers.add(target.targetEntityIdentifier());
          }
        }
        // Lexical sorting for target identifiers
        targetIdentifiers.sort(java.util.Comparator.naturalOrder());

        relations.add(new Relation(relation.id(), relation.name(),
            relation.targetTemplateIdentifier(), targetIdentifiers));
      }
      // Sort relations by name
      relations.sort(java.util.Comparator.comparing(Relation::name,
          java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

      return new Entity(projection.getId(), projection.getTemplateIdentifier(),
          projection.getName(), projection.getIdentifier(), properties, relations);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Failed to parse JSONB payload for entity: " + projection.getId(), e);
    }
  }

  private String jsonOrEmptyArray(String json) {
    return json == null || json.isBlank() ? "[]" : json;
  }

  private record PropertyProjection(@JsonProperty("id") UUID id, @JsonProperty("name") String name,
      @JsonProperty("value") String value) {
  }

  private record RelationProjection(@JsonProperty("id") UUID id, @JsonProperty("name") String name,
      @JsonProperty("targetTemplateIdentifier") String targetTemplateIdentifier,
      @JsonProperty("targetEntities") List<TargetProjection> targetEntities) {
    private RelationProjection {
      targetEntities = targetEntities != null ? targetEntities : List.of();
    }
  }

  private record TargetProjection(@JsonProperty("targetEntityUuid") UUID targetEntityUuid,
      @JsonProperty("targetEntityIdentifier") String targetEntityIdentifier) {
  }
}
