package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  @SuppressWarnings("null")
  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Entity> findEntityGraph(Collection<UUID> rootIds, int depth,
      boolean includeProperties, EntityGraphTraversalMode mode) {

    if (rootIds == null || rootIds.isEmpty()) {
      log.debug("[EntityGraphAdapter] Empty root IDs provided, returning empty map");
      return Map.of();
    }

    // Step 1: Collect all entity IDs in the graph via batch recursive CTE
    // Works for both single and multiple roots
    List<UUID> graphIds = jpaEntityRepository.findEntityIdsInGraph(rootIds, depth, mode.name());

    if (graphIds == null || graphIds.isEmpty()) {
      log.debug(
          "[EntityGraphAdapter] No graph identifiers found for roots (null or empty), returning empty map");
      return Map.of();
    }

    // Step 2: Extract unique identifiers for batch loading
    List<UUID> uniqueEntityIds = graphIds.stream().distinct().toList();

    // Step 3: Batch-load entities with relations, then optionally properties in a
    // separate query.
    // Properties are skipped when not requested to avoid the extra round-trip and
    // keep payloads lean.
    // The two-query split also avoids Hibernate's MultipleBagFetchException.
    List<EntityJpaEntity> jpaEntities = jpaEntityRepository
        .findAllByIdinWithRelations(uniqueEntityIds);
    if (includeProperties) {
      jpaEntityRepository.findAllByIdInWithProperties(uniqueEntityIds);
    }

    // Step 4: Map to domain and key by UUID for O(1) lookup
    return jpaEntities.stream().map(mapper::toDomain).filter(entity -> entity.id() != null)
        .collect(Collectors.toMap(Entity::id, Function.identity()));
  }

}
