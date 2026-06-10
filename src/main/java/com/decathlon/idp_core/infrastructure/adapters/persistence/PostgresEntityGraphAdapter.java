package com.decathlon.idp_core.infrastructure.adapters.persistence;

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
  public Map<UUID, Entity> findEntityGraph(UUID entityId, int depth, boolean includeProperties) {
    log.debug(
        "[EntityGraphAdapter] findEntityGraph start: entityId={}, depth={}, includeProperties={}",
        entityId, depth, includeProperties);
    final long tStartTotal = System.nanoTime();

    // Step 1: collect all (identifier, template_identifier) pairs via recursive
    // CTE.
    // The CTE always traverses ALL relation types to discover all reachable nodes.
    // Relation name filtering is applied at the service level when building edges,
    // so nodes reachable via any path are included even if the filter only matches
    // edges at deeper levels (e.g. filtering "owns" still returns B→C when A→B→C).
    final long tStartCte = System.nanoTime();
    List<UUID> graphPairs = jpaEntityRepository.findEntityUuidsInGraph(entityId, depth);
    final long tAfterCte = System.nanoTime();
    log.debug("[EntityGraphAdapter] CTE returned {} identifiers (elapsed={}ms)",
        graphPairs == null ? 0 : graphPairs.size(), (tAfterCte - tStartCte) / 1_000_000);

    if (graphPairs == null || graphPairs.isEmpty()) {
      log.debug(
          "[EntityGraphAdapter] No graph identifiers found (null or empty), returning empty map");
      return Map.of();
    }

    // Step 2: extract unique identifiers for batch loading
    final long tStartIdExtract = System.nanoTime();
    List<UUID> entitiesIds = graphPairs.stream().map(pair -> pair).distinct().toList();
    final long tAfterIdExtract = System.nanoTime();
    log.debug("[EntityGraphAdapter] Unique entity ids to load: {} (extraction elapsed={}ms)",
        entitiesIds.size(), (tAfterIdExtract - tStartIdExtract) / 1_000_000);

    // Step 3: batch-load entities with relations, then optionally properties in a
    // separate
    // query. Properties are skipped when not requested to avoid the extra
    // round-trip and
    // keep payloads lean. The two-query split also avoids Hibernate's
    // MultipleBagFetchException.
    log.debug("[EntityGraphAdapter] Loading JPA entities with relations...");
    final long tStartJpaLoad = System.nanoTime();
    List<EntityJpaEntity> jpaEntities = jpaEntityRepository
        .findAllByIdentifierInWithRelations(entitiesIds);
    final long tAfterJpaLoad = System.nanoTime();
    log.debug("[EntityGraphAdapter] Loaded {} JPA entities with relations (elapsed={}ms)",
        jpaEntities.size(), (tAfterJpaLoad - tStartJpaLoad) / 1_000_000);
    if (includeProperties) {
      log.debug("[EntityGraphAdapter] Loading properties for {} entities", entitiesIds.size());
      final long tStartProps = System.nanoTime();
      List<?> props = jpaEntityRepository.findAllByIdentifierInWithProperties(entitiesIds);
      final long tAfterProps = System.nanoTime();
      log.debug(
          "[EntityGraphAdapter] Properties load completed (result was {} entries, elapsed={}ms)",
          props == null ? 0 : props.size(), (tAfterProps - tStartProps) / 1_000_000);
    }

    // Step 4: map to domain and key by composite key for O(1) lookup
    log.debug("[EntityGraphAdapter] Mapping JPA entities to domain models...");
    final long tStartMap = System.nanoTime();

    Map<UUID, Entity> result = jpaEntities.stream().map(mapper::toDomain)
        .collect(Collectors.toMap(Entity::id, Function.identity()));
    final long tAfterMap = System.nanoTime();

    log.debug("[EntityGraphAdapter] Mapping completed, returning {} domain entities (elapsed={}ms)",
        result.size(), (tAfterMap - tStartMap) / 1_000_000);

    final long tEndTotal = System.nanoTime();
    log.debug("[EntityGraphAdapter] findEntityGraph end: entityId={} totalElapsed={}ms", entityId,
        (tEndTotal - tStartTotal) / 1_000_000);
    return result;
  }

}
