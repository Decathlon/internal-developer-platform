package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

import lombok.RequiredArgsConstructor;

/// Domain service for building entity relationship graphs.
///
/// Resolves outbound and inbound relations, for a given list of entities,
/// recursively up to a configurable depth, returning a tree of
/// [EntityGraphNode] records containing summary information for each connected
/// entity.
///
/// Phase 1 - Gathering the Raw information (DB & Footprint): Gets a list of all
/// possibly related entities from the database within a single transaction.
/// Phase 2 - Reachability Pruning (Breadth-First Propagation): It computes a
/// precise reachable footprint to ensure we only traverse nodes that actually
/// connect to our root in the selected direction. Phase 3 - Recursive
/// Construction (DFS with Safety Nets): The helper runs a depth-first traversal
/// starting at the root entity to build the nested EntityGraphNode tree. It
/// prevents stack overflows from circular references and avoid re-evaluating the
/// same nodes via different paths.
///
/// **Business purpose:**
/// - Visualizing entity dependency graphs in the catalog UI
/// - Understanding relationship chains (e.g., service → database →
///   infrastructure)
/// - Providing hierarchical views for impact analysis and change propagation
///
/// **Design decisions:**
/// - Uses depth-limited traversal to prevent unbounded recursion
/// - Optimized with recursive CTE and batch loading to minimize database queries
/// - A per-request `visitedNodeIds` set prevents exponential recursion: without
///   it, inbound relation scanning would re-expand already-visited nodes at
///   every depth level, producing O(2^depth) calls even for small graphs (OOM at
///   depth ≥ 10).
/// - Relation and property filtering are domain concerns applied during graph
///   construction, so that callers (e.g. the REST controller) receive a graph
///   that already respects the requested scope instead of carrying unnecessary
///   data to the Infrastructure layer.
///
///
///
@Service
@RequiredArgsConstructor
public class EntityGraphService {

  private final EntityRepositoryPort entityRepositoryPort;
  private final EntityTemplateValidationService entityTemplateValidationService;
  private final EntityService entityService;
  private final EntityGraphRepositoryPort entityGraphRepositoryPort;
  private final EntityGraphHelper entityGraphHelper;

  private static final int MAX_DEPTH = 6;

  /// Resolves a depth-limited, fully populated relationship graph for a single
  /// root entity.
  ///
  /// This method validates template existence, fetches the raw relational map in
  /// a
  /// single optimized DB query, and delegates the recursive tree building to the
  /// [EntityGraphHelper].
  ///
  /// @param templateIdentifier the template to find the root entity under (e.g.,
  /// "service")
  /// @param entityIdentifier the unique business identifier of the entity
  /// @param depth the requested depth of relationship traversal
  /// @param includeProperties whether to fetch property fields for the nodes
  /// @param relationFilter a set of relation names to restrict the traversal
  /// to (empty means all)
  /// @param propertyFilter a set of property names to restrict the properties
  /// to (empty means all)
  /// @param mode the traversal mode (e.g., `OUTBOUND_ONLY`,
  /// `BIDIRECTIONAL`)
  /// @return the resolved root [EntityGraphNode] with its relationship
  /// tree populated

  @Transactional(readOnly = true)
  public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier,
      int depth, boolean includeProperties, Set<String> relationFilter, Set<String> propertyFilter,
      EntityGraphTraversalMode mode) {

    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);

    entityTemplateValidationService.validateTemplateExists(templateIdentifier);

    // 1. Resolve root entity
    Entity rootEntity = entityRepositoryPort
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

    // 2. Load the graph footprint via optimized DB calls
    Map<UUID, Entity> entityMap = entityGraphRepositoryPort.findEntityGraph(Set.of(rootEntity.id()),
        effectiveDepth, includeProperties, mode);

    if (entityMap == null || entityMap.isEmpty()) {
      return new EntityGraphNode(rootEntity.templateIdentifier(), rootEntity.identifier(),
          rootEntity.name(), List.of(), List.of(), List.of());
    }

    // 3. Delegate to helper for graph building
    Map<UUID, EntityGraphNode> graphNodes = entityGraphHelper.buildGraphNodesForEntityIds(entityMap,
        includeProperties, relationFilter, propertyFilter, mode, effectiveDepth);

    return graphNodes.getOrDefault(rootEntity.id(),
        new EntityGraphNode(rootEntity.templateIdentifier(), rootEntity.identifier(),
            rootEntity.name(), List.of(), List.of(), List.of()));
  }

  /// Retrieves a paginated list of entity graphs for a given template in a single
  /// transaction.
  ///
  /// **Performance contract:** All queries — pagination, outbound and inbound
  /// relation lookups — execute within the same `@Transactional(readOnly = true)`
  /// boundary, avoiding split-transaction inconsistencies and N+1 problems.
  ///
  /// **Business purpose:** Provides complete bidirectional relationship context
  /// for paginated entity list responses, so the infrastructure mapper can
  /// produce
  /// unified `relations` DTOs without any further DB calls.
  ///
  /// @param pageable pagination and sorting configuration
  /// @param templateIdentifier template to scope the entity list
  /// @param entityFilter optional filter criteria; `null` means no filtering
  /// @param depth the requested depth of relationship traversal
  /// @return paginated graph nodes with outbound and inbound relations resolved
  /// @throws EntityTemplateNotFoundException when the template does not exist
  @SuppressWarnings("null")
  @Transactional(readOnly = true)
  public Page<EntityGraphNode> getEntityGraphPageByTemplate(Pageable pageable,
      String templateIdentifier, EntityFilter entityFilter, int depth) {

    // Fetch paginated entities — template existence is validated inside this call
    Page<Entity> entityPage = entityService.getEntitiesByTemplateIdentifier(pageable,
        templateIdentifier, entityFilter);

    if (entityPage.isEmpty()) {
      // Return empty page preserving pagination metadata
      return entityPage.map(entity -> new EntityGraphNode(entity.templateIdentifier(),
          entity.identifier(), entity.name(), entity.properties(), List.of(), List.of()));
    }

    // Extract UUIDs for the batch graph call
    var entityUuids = entityPage.getContent().stream().map(Entity::id).filter(Objects::nonNull)
        .toList();

    // Load entity graphs in batch (includes roots + neighbors for relation
    // resolution)
    Map<UUID, Entity> entityGraphs = entityGraphRepositoryPort.findEntityGraph(entityUuids, depth,
        true, EntityGraphTraversalMode.DIRECT_LINEAGE);

    // Call the helper to build graph nodes
    Map<UUID, EntityGraphNode> graphsByUuid = entityGraphHelper.buildGraphNodesForEntityIds(
        entityGraphs, true, Set.of(), Set.of(), EntityGraphTraversalMode.DIRECT_LINEAGE, depth);

    // Map each Entity to its EntityGraphNode, falling back gracefully if missing
    return entityPage.map(entity -> graphsByUuid.getOrDefault(entity.id(),
        new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
            entity.properties(), List.of(), List.of())));
  }

}
