package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public class PostgresEntityGraphAdapter implements EntityGraphRepositoryPort {

  private final JpaEntityRepository jpaEntityRepository;
  private final EntityPersistenceMapper mapper;

  private static final Logger log = LoggerFactory.getLogger(PostgresEntityGraphAdapter.class);

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Entity> findEntityGraph(UUID entityId, int depth, boolean includeProperties,
      EntityGraphTraversalMode mode) {

    // Step 1: collect all (identifier, template_identifier) pairs via recursive
    // CTE.
    // The CTE always traverses ALL relation types to discover all reachable nodes.
    // Relation name filtering is applied at the service level when building edges,
    // so nodes reachable via any path are included even if the filter only matches
    // edges at deeper levels (e.g. filtering "owns" still returns B→C when A→B→C).
    List<UUID> graphPairs = jpaEntityRepository.findEntityIdsInGraph(entityId, depth, mode.name());

    if (graphPairs == null || graphPairs.isEmpty()) {
      log.debug(
          "[EntityGraphAdapter] No graph identifiers found (null or empty), returning empty map");
      return Map.of();
    }

    // Step 2: extract unique identifiers for batch loading
    List<UUID> entitiesIds = graphPairs.stream().map(pair -> pair).distinct().toList();

    // Step 3: batch-load entities with relations, then optionally properties in a
    // separate query.
    // Properties are skipped when not requested to avoid the extra round-trip and
    // keep payloads lean.
    // The two-query split also avoids Hibernate's MultipleBagFetchException.
    List<EntityJpaEntity> jpaEntities = jpaEntityRepository.findAllByIdinWithRelations(entitiesIds);
    if (includeProperties) {
      jpaEntityRepository.findAllByIdInWithProperties(entitiesIds);
    }

    // Step 4: map to domain and key by composite key for O(1) lookup
    return jpaEntities.stream().map(mapper::toDomain)
        .collect(Collectors.toMap(Entity::id, Function.identity()));

  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Entity> findEntityGraphBatch(java.util.List<UUID> rootIds, int depth,
      boolean includeProperties, EntityGraphTraversalMode mode) {

    if (rootIds == null || rootIds.isEmpty()) {
      log.debug("[EntityGraphAdapter] Empty root IDs list provided, returning empty map");
      return Map.of();
    }

    // Step 1: collect all entity IDs in the graph for all root IDs via batch
    // recursive CTE
    List<UUID> graphIds = jpaEntityRepository.findEntityGraphIdentifiersBatch(rootIds, depth,
        mode.name());

    if (graphIds == null || graphIds.isEmpty()) {
      log.debug(
          "[EntityGraphAdapter] No graph identifiers found for batch roots (null or empty), returning empty map");
      return Map.of();
    }

    // Step 2: extract unique identifiers for batch loading
    List<UUID> entitiesIds = graphIds.stream().distinct().toList();

    // Step 3: batch-load entities with relations, then optionally properties in a
    // separate query.
    // Properties are skipped when not requested to avoid the extra round-trip and
    // keep payloads lean.
    // The two-query split also avoids Hibernate's MultipleBagFetchException.
    List<EntityJpaEntity> jpaEntities = jpaEntityRepository.findAllByIdinWithRelations(entitiesIds);
    if (includeProperties) {
      jpaEntityRepository.findAllByIdInWithProperties(entitiesIds);
    }

    // Step 4: map to domain and key by UUID for O(1) lookup
    return jpaEntities.stream().map(mapper::toDomain)
        .collect(Collectors.toMap(Entity::id, Function.identity()));

  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Entity> findEntityGraphBatchByTemplate(java.util.List<UUID> rootIds, int depth,
      String startTemplate, int size, int offset) {

    if (rootIds == null || rootIds.isEmpty()) {
      log.debug("[EntityGraphAdapter] Empty root IDs list provided, returning empty map");
      return Map.of();
    }

    // Step 1: collect all entity IDs in the graph for all root IDs via batch
    // recursive CTE
    List<UUID> graphIds = jpaEntityRepository.findEntityGraphIdsByTemplate(rootIds, depth,
        startTemplate, size, offset);

    if (graphIds == null || graphIds.isEmpty()) {
      log.debug(
          "[EntityGraphAdapter] No graph identifiers found for batch roots (null or empty), returning empty map");
      return Map.of();
    }

    // Step 2: extract unique identifiers for batch loading
    List<UUID> entitiesIds = graphIds.stream().distinct().toList();

    // Step 3: batch-load entities with relations, then optionally properties in a
    // separate query.
    // Properties are skipped when not requested to avoid the extra round-trip and
    // keep payloads lean.
    // The two-query split also avoids Hibernate's MultipleBagFetchException.
    List<EntityJpaEntity> jpaEntities = jpaEntityRepository.findAllByIdinWithRelations(entitiesIds);
    // if (includeProperties) {
    // jpaEntityRepository.findAllByIdInWithProperties(entitiesIds);
    // }

    // Step 4: map to domain and key by UUID for O(1) lookup
    return jpaEntities.stream().map(mapper::toDomain)
        .collect(Collectors.toMap(Entity::id, Function.identity()));

  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Entity> findEntityGraphByAgnosticTemplate(UUID[] rootIds, String[] groupIds,
      long expectedGroupCount, int depth, String startTemplate, int size, int offset) {

    if (rootIds == null || rootIds.length == 0) {
      log.debug("[EntityGraphAdapter] Empty root IDs list provided, returning empty map");
      return Map.of();
    }

    // Step 1: collect all entity IDs in the graph for all root IDs via batch
    // recursive CTE
    List<UUID> graphIds = jpaEntityRepository.findEntityGraphIdsAgnosticIntersect(rootIds, groupIds,
        expectedGroupCount, depth, startTemplate, size, offset);

    if (graphIds == null || graphIds.isEmpty()) {
      log.debug(
          "[EntityGraphAdapter] No graph identifiers found for batch roots (null or empty), returning empty map");
      return Map.of();
    }

    // Step 2: extract unique identifiers for batch loading
    List<UUID> entitiesIds = graphIds.stream().distinct().toList();

    // Step 3: batch-load entities with relations, then optionally properties in a
    // separate query.
    // Properties are skipped when not requested to avoid the extra round-trip and
    // keep payloads lean.
    // The two-query split also avoids Hibernate's MultipleBagFetchException.
    List<EntityJpaEntity> jpaEntities = jpaEntityRepository.findAllByIdinWithRelations(entitiesIds);
    // if (includeProperties) {
    // jpaEntityRepository.findAllByIdInWithProperties(entitiesIds);
    // }

    // Step 4: map to domain and key by UUID for O(1) lookup
    return jpaEntities.stream().map(mapper::toDomain)
        .collect(Collectors.toMap(Entity::id, Function.identity()));

  }
}
