package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityCompositeKey;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphQueryOptions;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity.EntityService;

import lombok.RequiredArgsConstructor;

/// Domain service for building entity relationship graphs.
///
/// Resolves outbound and inbound relations, for a given list of entities,
/// recursively up to a configurable depth, returning a tree of
/// [EntityGraphNode] records containing summary information for each connected
/// entity.
///
/// For a single root, loads a depth-limited entity footprint and delegates
/// reachability pruning and graph-node construction to [EntityGraphHelper]. For
/// template pages, loads graph footprints for the page's entities in one batch.
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
/// - Relation and property-name filtering are applied during graph-node
///   construction. Property fetching itself is controlled by the repository
///   flags so unrequested property data is not loaded.
@Service
@RequiredArgsConstructor
public class EntityGraphService {

  private final EntityRepositoryPort entityRepositoryPort;
  private final EntityService entityService;
  private final EntityGraphRepositoryPort entityGraphRepositoryPort;
  private final EntityGraphHelper entityGraphHelper;

  private static final int MAX_DEPTH = 6;

  /// Resolves a depth-limited, fully populated relationship graph for a single
  /// root entity.
  ///
  /// Resolves the root entity summary, fetches the graph footprint with the
  /// requested property scope, and delegates graph-node construction to
  /// [EntityGraphHelper]. The service clamps this call's depth to 1 through 6.
  ///
  /// @param templateIdentifier the template to find the root entity under (e.g.,
  /// "service")
  /// @param entityIdentifier the unique business identifier of the entity
  /// @param options traversal depth, property selection, relation and property
  /// filters, and traversal mode
  /// @return the resolved root [EntityGraphNode] with its filtered relations and
  /// available properties
  /// @throws EntityNotFoundException if no root matches the template and entity
  /// identifiers

  @Transactional(readOnly = true)
  public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier,
      EntityGraphQueryOptions options) {

    int effectiveDepth = Math.clamp(options.depth(), 1, MAX_DEPTH);

    EntitySummary rootEntity = entityRepositoryPort
        .findSummariesByCompositeKeys(
            List.of(new EntityCompositeKey(templateIdentifier, entityIdentifier)))
        .stream().findFirst()
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

    // 2. Load the graph footprint via optimized DB calls
    Map<UUID, Entity> entityMap = entityGraphRepositoryPort.findEntityGraph(Set.of(rootEntity.id()),
        effectiveDepth, options.includeProperties(), options.includeRootEntityProperties(),
        options.mode());

    if (entityMap == null || entityMap.isEmpty()) {
      return new EntityGraphNode(rootEntity.templateIdentifier(), rootEntity.identifier(),
          rootEntity.name(), List.of(), List.of(), List.of());
    }

    // 3. Delegate to helper for graph building
    Map<UUID, EntityGraphNode> graphNodes = entityGraphHelper.buildGraphNodesForEntityIds(entityMap,
        options.relationFilter(), options.propertyFilter(), options.mode(), effectiveDepth);

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
  /// @param paginationCriteria zero-based page, page size (1 to MAX_PAGE_SIZE),
  /// and the sort is not applied
  /// @param templateIdentifier template to scope the entity list
  /// @param entityFilter optional filter criteria; `null` means no filtering
  /// @param depth the maximum relationship depth used for each page entity's
  /// graph traversal
  /// @return paginated graph nodes with outbound and inbound relations resolved
  /// @throws EntityTemplateNotFoundException when the template does not exist
  /// @throws InvalidSearchQueryException when page or size is out of bounds
  @SuppressWarnings("null")
  @Transactional(readOnly = true)
  public Page<EntityGraphNode> getEntityGraphPageByTemplate(PaginationCriteria paginationCriteria,
      String templateIdentifier, EntityFilter entityFilter, int depth) {

    paginationCriteria.validateBounds();
    Pageable pageable = PageRequest.of(paginationCriteria.page(), paginationCriteria.size());

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
        false, true, EntityGraphTraversalMode.DIRECT_LINEAGE);

    // Call the helper to build graph nodes
    Map<UUID, EntityGraphNode> graphsByUuid = entityGraphHelper.buildGraphNodesForEntityIds(
        entityGraphs, Set.of(), Set.of(), EntityGraphTraversalMode.DIRECT_LINEAGE, depth);

    // Map each Entity to its EntityGraphNode, falling back gracefully if missing
    return entityPage.map(entity -> graphsByUuid.getOrDefault(entity.id(),
        new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
            entity.properties(), List.of(), List.of())));
  }

}
