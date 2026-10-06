package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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

/// Persistence adapter dedicated to entity relationship graph traversal.
///
/// Separated from [PostgresEntityAdapter] because graph queries use a distinct
/// recursive CTE strategy that has no overlap with standard CRUD operations,
/// following the Interface Segregation Principle.
///
/// **Query strategy:**
/// 1. One recursive CTE query to collect all (identifier, template_identifier)
///    pairs in the graph.
/// 2. One batch query to load entities with their relations (avoids N+1).
/// 3. One batch query to load properties separately
///    (avoids MultipleBagFetchException).
@Component
public class PostgresEntityGraphAdapter implements EntityGraphRepositoryPort {

  private final JpaEntityRepository jpaEntityRepository;
  private final ObjectReader propertyReader;
  private final ObjectReader relationReader;

  public PostgresEntityGraphAdapter(JpaEntityRepository jpaEntityRepository,
      ObjectMapper objectMapper) {
    this.jpaEntityRepository = jpaEntityRepository;
    this.propertyReader = objectMapper.readerFor(new TypeReference<List<PropertyProjection>>() {
    });
    this.relationReader = objectMapper.readerFor(new TypeReference<List<RelationProjection>>() {
    });
  }

  /// Fetches a depth-limited entity relationship graph for one or more root
  /// entities.
  ///
  /// **Purpose:** Implements the persistence contract for graph traversal by
  /// executing
  /// a recursive CTE query followed by targeted batch loads. This method bridges
  /// the
  /// Domain Service (which calls this port) with the database, handling all
  /// technical
  /// concerns: query execution, result materialization, and entity mapping.
  ///
  /// **Three-step strategy:**
  /// 1. **Graph discovery**: Execute recursive CTE to find all reachable entity
  /// UUIDs
  /// within the depth limit, respecting the traversal mode (OUTBOUND,
  /// BIDIRECTIONAL, etc.)
  /// 2. **Batch entity load**: Fetch all discovered entities with their relations
  /// in a
  /// single query to avoid N+1 problems
  /// 3. **Optional properties load**: If requested, load properties separately to
  /// avoid
  /// Hibernate's MultipleBagFetchException when combining multiple `@OneToMany`
  /// collections
  ///
  /// **Why separate queries for properties?**
  /// - Hibernate cannot safely join multiple collection-valued associations in a
  /// single
  /// query without cartesian products
  /// - Properties are often not needed (e.g., list endpoints just show names)
  /// - Splitting queries keeps payloads lean and avoids unnecessary data transfer
  ///
  /// @param rootIds the UUIDs of entities to use as traversal roots (single or
  /// multiple)
  /// @param depth maximum levels to traverse (clamped by domain layer to [1,
  /// MAX_DEPTH])
  /// @param includeProperties whether to fetch property data for each entity
  /// @param mode traversal direction (OUTBOUND_ONLY, BIDIRECTIONAL,
  /// DIRECT_LINEAGE)
  /// @return immutable map of all discovered entities keyed by UUID; empty map if
  /// no
  /// entities found or if input is null/empty
  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Entity> findEntityGraph(Collection<UUID> rootIds, int depth,
      boolean includeProperties, EntityGraphTraversalMode mode) {
    List<UUID> discoveredIds = jpaEntityRepository.findEntityIdsInGraph(rootIds, depth,
        mode.name());
    if (discoveredIds.isEmpty()) {
      return Map.of();
    }

    List<EntityGraphJsonProjection> projections = jpaEntityRepository
        .findEntityGraphDataByIds(discoveredIds);
    Map<UUID, Entity> entities = HashMap.newHashMap(projections.size());
    for (EntityGraphJsonProjection projection : projections) {
      Entity entity = mapProjectionToDomain(projection);
      if (entities.putIfAbsent(entity.id(), entity) != null) {
        throw new IllegalStateException("Duplicate entity ID in graph projection: " + entity.id());
      }
    }
    return entities;
  }

  private Entity mapProjectionToDomain(EntityGraphJsonProjection projection) {
    try {
      List<PropertyProjection> propertyProjections = propertyReader
          .readValue(jsonOrEmptyArray(projection.getPropertiesJson()));
      List<RelationProjection> relationProjections = relationReader
          .readValue(jsonOrEmptyArray(projection.getRelationsJson()));

      List<Property> properties = new java.util.ArrayList<>(propertyProjections.size());
      for (PropertyProjection property : propertyProjections) {
        properties.add(new Property(property.id(), property.name(), property.value()));
      }

      List<Relation> relations = new java.util.ArrayList<>(relationProjections.size());
      for (RelationProjection relation : relationProjections) {
        List<String> targetIdentifiers = new java.util.ArrayList<>(
            relation.targetEntities().size());
        for (TargetProjection target : relation.targetEntities()) {
          if (Objects.nonNull(target.targetEntityIdentifier())) {
            targetIdentifiers.add(target.targetEntityIdentifier());
          }
        }
        relations.add(new Relation(relation.id(), relation.name(),
            relation.targetTemplateIdentifier(), targetIdentifiers));
      }

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
