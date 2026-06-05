package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

import lombok.RequiredArgsConstructor;

/// Domain service for building entity relationship graphs.
///
/// Resolves an entity's outbound and inbound relations recursively up to a
/// configurable depth, returning a tree of [EntityGraphNode] records containing
/// summary information for each connected entity.
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
@Service
@RequiredArgsConstructor
public class EntityGraphService {

  private static final Logger log = LoggerFactory.getLogger(EntityGraphService.class);
  private static final int MAX_DEPTH = 20;

  private final EntityRepositoryPort entityRepositoryPort;
  private final EntityGraphRepositoryPort entityGraphRepositoryPort;
  private final EntityTemplateValidationService entityTemplateValidationService;

  @Transactional(readOnly = true)
  public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier,
      int depth, boolean includeProperties, Set<String> relationFilter,
      Set<String> propertyFilter) {
    final long tStartTotal = System.nanoTime();

    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);

    // Resolve root entity and measure time
    final long tStartResolve = System.nanoTime();
    Entity rootEntity = entityRepositoryPort
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));
    final long tAfterResolve = System.nanoTime();
    // log.debug("[EntityGraph] Resolved root entity: id='{}' identifier='{}'
    // template='{}' (elapsed={}ms)", rootEntity.id(), rootEntity.identifier(),
    // rootEntity.templateIdentifier(), (tAfterResolve - tStartResolve) /
    // 1_000_000);

    // Load entire graph chunk via optimized DB calls
    final long tStartRepo = System.nanoTime();
    Map<UUID, Entity> entityMap = entityGraphRepositoryPort.findEntityGraph(rootEntity.id(),
        effectiveDepth, includeProperties);
    final long tAfterRepo = System.nanoTime();

    if (entityMap == null || entityMap.isEmpty()) {
      log.debug(
          "[EntityGraph] No entities returned from repository for root id='{}'. Returning single-node graph. (repoElapsed={}ms)",
          rootEntity.id(), (tAfterRepo - tStartRepo) / 1_000_000);
      final long tEndTotalEmpty = System.nanoTime();
      log.debug("[EntityGraph] getEntityGraph end (single-node): totalElapsed={}ms",
          (tEndTotalEmpty - tStartTotal) / 1_000_000);
      return new EntityGraphNode(rootEntity.id().toString(), rootEntity.identifier(),
          rootEntity.name(), List.of(), List.of(), List.of());
    }

    log.debug(
        "[EntityGraph] Repository returned {} entities for root id='{}' (includeProperties={}) repoElapsed={}ms",
        entityMap.size(), rootEntity.id(), includeProperties,
        (tAfterRepo - tStartRepo) / 1_000_000);

    // -------------------------------------------------------------------------
    // BULK PRE-COMPUTATION LAYER (Normalized for absolute string resilience)
    // -------------------------------------------------------------------------
    final long tStartIndex = System.nanoTime();
    IndexBundle indices = buildIndices(entityMap);
    final long tAfterIndex = System.nanoTime();

    Map<EntityCompositeKey, UUID> textToUuidLookup = indices.textToUuidLookup();
    Map<String, Map<String, List<UUID>>> inboundIndex = indices.inboundIndex();
    int inboundEntries = inboundIndex.values().stream()
        .mapToInt(m -> m.values().stream().mapToInt(List::size).sum()).sum();
    // log.debug("[EntityGraph][Index] Built textToUuidLookup size={}
    // inboundIndexRelations={} totalInboundSources={} (processed={})
    // indexElapsed={}ms",
    // textToUuidLookup.size(), inboundIndex.size(), inboundEntries,
    // entityMap.size(), (tAfterIndex - tStartIndex) / 1_000_000);

    // Depth-Aware Tracker to prevent premature branch starvation
    Map<String, Integer> visitedDepths = new HashMap<>();

    // Create context object to avoid long parameter lists when traversing
    GraphTraversalContext ctx = new GraphTraversalContext(entityMap, textToUuidLookup, inboundIndex,
        includeProperties, propertyFilter, relationFilter, visitedDepths);

    // Trigger recursion passing our resilient indices via context
    // log.debug("[EntityGraph] Starting recursive graph build from root id='{}'
    // with depthBudget={}'", rootEntity.id(), effectiveDepth);
    final long tStartRecursion = System.nanoTime();
    EntityGraphNode rootNode = buildGraphNode(rootEntity.id(), ctx, effectiveDepth);
    final long tAfterRecursion = System.nanoTime();
    // log.debug("[EntityGraph] Completed recursive graph build for root id='{}'.
    // recursionElapsed={}ms Visited {} nodes.", rootEntity.id(), (tAfterRecursion -
    // tStartRecursion) / 1_000_000, visitedDepths.size());

    // Log unvisited entities for diagnostics
    logUnvisitedEntities(entityMap, visitedDepths);

    final long tEndTotal = System.nanoTime();
    log.debug(
        "[EntityGraph] getEntityGraph end: totalElapsed={}ms (resolve={}ms repo={}ms index={}ms recursion={}ms)",
        (tEndTotal - tStartTotal) / 1_000_000, (tAfterResolve - tStartResolve) / 1_000_000,
        (tAfterRepo - tStartRepo) / 1_000_000, (tAfterIndex - tStartIndex) / 1_000_000,
        (tAfterRecursion - tStartRecursion) / 1_000_000);

    return rootNode;
  }

  /// Logs entities that were loaded but never visited during graph traversal.
  /// This helps diagnose why the number of loaded entities exceeds the number of
  /// output nodes.
  private void logUnvisitedEntities(Map<UUID, Entity> entityMap,
      Map<String, Integer> visitedDepths) {
    Set<UUID> visitedUuids = new HashSet<>();
    for (String uuidStr : visitedDepths.keySet()) {
      try {
        visitedUuids.add(UUID.fromString(uuidStr));
      } catch (IllegalArgumentException _) {
        // Invalid UUID format, skip
      }
    }

    List<Entity> unvisitedEntities = entityMap.entrySet().stream()
        .filter(entry -> !visitedUuids.contains(entry.getKey())).map(Map.Entry::getValue)
        .filter(Objects::nonNull).toList();

    if (!unvisitedEntities.isEmpty()) {
      // log.info("[EntityGraph] Loaded {} entities, visited {} entities, {} entities
      // were unreachable",
      // entityMap.size(), visitedUuids.size(), unvisitedEntities.size());

      // Group by template for better readability
      Map<String, List<String>> unvisitedByTemplate = new HashMap<>();
      for (Entity entity : unvisitedEntities) {
        unvisitedByTemplate.computeIfAbsent(entity.templateIdentifier(), k -> new ArrayList<>())
            .add(entity.identifier());
      }

      // unvisitedByTemplate.forEach((template, identifiers) ->
      // log.info(" Template '{}': {} entities - {}",
      // template, identifiers.size(),
      // identifiers.size() <= 10 ? identifiers : identifiers.subList(0, 10) + "... ("
      // + (identifiers.size() - 10) + " more)" )
      // );
    } else {
      log.info("[EntityGraph] All {} loaded entities were visited (100% reachability)",
          entityMap.size());
    }
  }

  private EntityGraphNode buildGraphNode(UUID entityUuid, Map<UUID, Entity> entityMap,
      Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex, int remainingDepth,
      boolean includeProperties, Set<String> propertyFilter, Set<String> relationFilter,
      Map<String, Integer> visitedDepths) {
    // Note: This method signature is replaced by the Context-based overload below
    // during refactor.
    // Kept for backward-compatibility for the insert edit tool to apply minimal
    // changes.
    Entity entity = entityMap.get(entityUuid);

    var nodeIdDisplay = entityUuid != null ? entityUuid.toString() : "null-entity-";
    if (entity == null) {
      log.trace("[EntityGraph][buildGraphNode] Missing entity for uuid='{}'. Returning empty node.",
          nodeIdDisplay);
      return new EntityGraphNode(nodeIdDisplay, nodeIdDisplay, nodeIdDisplay, List.of(), List.of(),
          List.of());
    }

    log.trace("[EntityGraph][buildGraphNode] Enter node='{}' identifier='{}' remainingDepth={}",
        entity.id(), entity.identifier(), remainingDepth);

    // Check depth budget exhaustion first
    if (remainingDepth <= 0) {
      log.trace(
          "[EntityGraph][buildGraphNode] Depth exhausted at node='{}'. Resolving leaf properties only.",
          entity.identifier());
      List<Property> leafProperties = resolveProperties(entity, includeProperties, propertyFilter);
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          leafProperties, List.of(), List.of());
    }

    // Depth-Aware Cycle Breaking Guard
    var nodeId = entity.id().toString();
    Integer previousMaxDepthBudget = visitedDepths.get(nodeId);

    if (previousMaxDepthBudget != null && previousMaxDepthBudget >= remainingDepth) {
      log.trace(
          "[EntityGraph][buildGraphNode] Node '{}' already visited with equal or larger budget (prev={} curr={}). Returning stub.",
          entity.identifier(), previousMaxDepthBudget, remainingDepth);
      List<Property> stubProperties = resolveProperties(entity, includeProperties, propertyFilter);
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          stubProperties, List.of(), List.of());
    }

    visitedDepths.put(nodeId, remainingDepth);

    // Process outbound relationships
    List<EntityGraphRelation> outboundRelations = entity.relations().stream()
        .filter(relation -> relationFilter.isEmpty() || relationFilter.contains(relation.name()))
        .map(relation -> new EntityGraphRelation(relation.name(),
            relation.targetEntityIdentifiers().stream().map(targetId -> {
              // Look up using normalized coordinates
              UUID targetUuid = textToUuidLookup
                  .get(new EntityCompositeKey(relation.targetTemplateIdentifier(), targetId));
              if (targetUuid == null)
                return null;

              return buildGraphNode(targetUuid, entityMap, textToUuidLookup, inboundIndex,
                  remainingDepth, includeProperties, propertyFilter, relationFilter, visitedDepths);
            }).filter(Objects::nonNull).toList()))
        .filter(rel -> !rel.targets().isEmpty()).toList();

    log.trace("[EntityGraph][buildGraphNode] Node='{}' outboundRelations={} (after filtering)",
        entity.identifier(), outboundRelations.size());

    // Process inbound relationships
    // Use the new context-based method to build inbound relations
    GraphTraversalContext ctx = new GraphTraversalContext(entityMap, textToUuidLookup, inboundIndex,
        includeProperties, propertyFilter, relationFilter, visitedDepths);
    List<EntityGraphRelation> inboundRelations = buildRelationsAsTargetFromIndex(
        entity.identifier(), ctx, remainingDepth);

    log.trace("[EntityGraph][buildGraphNode] Node='{}' inboundRelations={} (after index lookup)",
        entity.identifier(), inboundRelations.size());

    List<Property> properties = resolveProperties(entity, includeProperties, propertyFilter);
    log.trace(
        "[EntityGraph][buildGraphNode] Leaving node='{}' propertiesCount={} totalRelationsOut={} totalRelationsIn={}",
        entity.identifier(), properties.size(), outboundRelations.size(), inboundRelations.size());
    return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
        properties, outboundRelations, inboundRelations);
  }

  private List<EntityGraphRelation> buildRelationsAsTargetFromIndex(String targetIdentifier,
      GraphTraversalContext ctx, int remainingDepth) {
    Map<String, Map<String, List<UUID>>> inboundIndex = ctx.inboundIndex();
    Set<String> relationFilter = ctx.relationFilter();

    // Normalize the map query coordinate to guarantee matching across variations
    String normalizedTargetIdentifier = targetIdentifier == null
        ? ""
        : targetIdentifier.trim().toLowerCase();
    Map<String, List<UUID>> sourcesByRelationName = inboundIndex
        .getOrDefault(normalizedTargetIdentifier, Map.of());

    if (sourcesByRelationName.isEmpty()) {
      log.trace(
          "[EntityGraph][buildRelations] No inbound sources found for target='{}' (normalized='{}')",
          targetIdentifier, normalizedTargetIdentifier);
      return List.of();
    }

    log.trace("[EntityGraph][buildRelations] Found {} relation entries for target='{}'",
        sourcesByRelationName.size(), targetIdentifier);

    return sourcesByRelationName.entrySet().stream()
        .filter(e -> relationFilter.isEmpty() || relationFilter.contains(e.getKey())).map(e -> {
          log.trace(
              "[EntityGraph][buildRelations] Processing inbound relation='{}' with {} sources for target='{}'",
              e.getKey(), e.getValue().size(), targetIdentifier);
          List<EntityGraphNode> targets = e.getValue().stream()
              .map(sourceUuid -> buildGraphNode(sourceUuid, ctx, remainingDepth - 1)).toList();
          return new EntityGraphRelation(e.getKey(), targets);
        }).toList();
  }

  private List<Property> resolveProperties(Entity entity, boolean includeProperties,
      Set<String> propertyFilter) {
    if (!includeProperties) {
      log.trace(
          "[EntityGraph][properties] Skipping property resolution for entity='{}' (includeProperties=false)",
          entity == null ? "null" : entity.identifier());
      return List.of();
    }
    if (propertyFilter.isEmpty()) {
      log.trace("[EntityGraph][properties] Including all properties for entity='{}' count={}",
          entity.identifier(), entity.properties().size());
      return entity.properties();
    }
    List<Property> resolved = entity.properties().stream()
        .filter(p -> propertyFilter.contains(p.name())).toList();
    log.trace(
        "[EntityGraph][properties] Resolved {} properties from filter size={} for entity='{}'",
        resolved.size(), propertyFilter.size(), entity.identifier());
    return resolved;
  }

  // New context and index bundle records to reduce parameter passing and cluster
  // related state
  private static record IndexBundle(Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex) {
  }

  private static record GraphTraversalContext(Map<UUID, Entity> entityMap,
      Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex, boolean includeProperties,
      Set<String> propertyFilter, Set<String> relationFilter, Map<String, Integer> visitedDepths) {
  }

  private IndexBundle buildIndices(Map<UUID, Entity> entityMap) {
    Map<EntityCompositeKey, UUID> textToUuidLookup = new HashMap<>();
    Map<String, Map<String, List<UUID>>> inboundIndex = new HashMap<>();
    int processedEntities = 0;
    for (Map.Entry<UUID, Entity> entry : entityMap.entrySet()) {
      UUID sourceUuid = entry.getKey();
      Entity entity = entry.getValue();
      processedEntities++;
      if (entity == null) {
        log.trace("[EntityGraph][Index] Skipping null entity for uuid='{}'", sourceUuid);
        continue;
      }

      // Build Index 1 (Automated normalization happens inside the record constructor
      // below)
      textToUuidLookup.put(new EntityCompositeKey(entity.templateIdentifier(), entity.identifier()),
          sourceUuid);

      // Build Index 2 (Normalized to lowercase and trimmed to eliminate trailing
      // space bugs)
      for (Relation relation : entity.relations()) {
        for (String targetId : relation.targetEntityIdentifiers()) {
          if (targetId == null)
            continue;
          String normalizedTargetId = targetId.trim().toLowerCase();
          inboundIndex.computeIfAbsent(normalizedTargetId, k -> new HashMap<>())
              .computeIfAbsent(relation.name(), k -> new ArrayList<>()).add(sourceUuid);
        }
        log.trace("[EntityGraph][Index] Entity '{}' added relation '{}' with {} targets",
            entity.identifier(), relation.name(), relation.targetEntityIdentifiers().size());
      }

      if (log.isTraceEnabled() && processedEntities % 500 == 0) {
        log.trace("[EntityGraph][Index] Processed {} entities so far (current uuid='{}')",
            processedEntities, sourceUuid);
      }
    }

    return new IndexBundle(textToUuidLookup, inboundIndex);
  }

  // Context-based overload of buildGraphNode to reduce parameter count
  private EntityGraphNode buildGraphNode(UUID entityUuid, GraphTraversalContext ctx,
      int remainingDepth) {
    return buildGraphNode(entityUuid, ctx.entityMap(), ctx.textToUuidLookup(), ctx.inboundIndex(),
        remainingDepth, ctx.includeProperties(), ctx.propertyFilter(), ctx.relationFilter(),
        ctx.visitedDepths());
  }
}

// SOLUTION FIX: Enforced lowercase, trimmed normalization inside the
// constructor
record EntityCompositeKey(String templateIdentifier, String identifier) {
  public EntityCompositeKey {
    templateIdentifier = templateIdentifier == null ? "" : templateIdentifier.trim().toLowerCase();
    identifier = identifier == null ? "" : identifier.trim().toLowerCase();
  }
}
