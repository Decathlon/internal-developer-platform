package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
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

  private static final int MAX_DEPTH = 6;

  private final EntityRepositoryPort entityRepositoryPort;
  private final EntityGraphRepositoryPort entityGraphRepositoryPort;
  private final EntityTemplateValidationService entityTemplateValidationService;

  @Transactional(readOnly = true)
  public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier,
      int depth, boolean includeProperties, Set<String> relationFilter, Set<String> propertyFilter,
      EntityGraphTraversalMode mode) {

    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);

    Entity rootEntity = entityRepositoryPort
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

    Map<UUID, Entity> entityMap = entityGraphRepositoryPort.findEntityGraph(rootEntity.id(),
        effectiveDepth, includeProperties, mode);

    if (entityMap == null || entityMap.isEmpty()) {
      return new EntityGraphNode(rootEntity.templateIdentifier(), rootEntity.identifier(),
          rootEntity.name(), List.of(), List.of(), List.of());
    }

    IndexBundle indices = buildIndices(entityMap, mode);

    Set<String> activeStack = new HashSet<>();
    Map<String, EntityGraphNode> memoCache = new HashMap<>();

    GraphTraversalContext ctx = new GraphTraversalContext(entityMap, indices.textToUuidLookup(),
        indices.inboundIndex(), includeProperties, propertyFilter, relationFilter, activeStack,
        memoCache, mode);

    return buildGraphNode(rootEntity.id(), ctx);
  }

  /// Retrieves entity graphs for multiple root entities in a single batch
  /// operation.
  @Transactional(readOnly = true)
  public Map<String, EntityGraphNode> getBatchEntityGraphsByIdentifiers(
      Map<UUID, Entity> entityGraphs, int depth, boolean includeProperties,
      Set<String> relationFilter, Set<String> propertyFilter, EntityGraphTraversalMode mode) {

    if (entityGraphs == null || entityGraphs.isEmpty()) {
      return Map.of();
    }

    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);
    IndexBundle globalIndices = buildIndices(entityGraphs, mode);
    Map<String, EntityGraphNode> result = new HashMap<>();

    for (Map.Entry<UUID, Entity> entry : entityGraphs.entrySet()) {
      Entity entity = entry.getValue();
      if (entity != null) {
        // 1. Isolate the footprint of entities reachable strictly by THIS root node
        Set<UUID> reachableFootprint = computeReachableSubGraph(entry.getKey(), entityGraphs,
            globalIndices, effectiveDepth, mode);

        // 2. Filter down to a localized sub-map
        Map<UUID, Entity> localizedEntityMap = entityGraphs.entrySet().stream()
            .filter(e -> reachableFootprint.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 3. Rebuild fresh indices completely clean of cross-contamination
        IndexBundle localizedIndices = buildIndices(localizedEntityMap, mode);

        Set<String> isolatedStack = new HashSet<>();
        Map<String, EntityGraphNode> isolatedCache = new HashMap<>();

        GraphTraversalContext localizedCtx = new GraphTraversalContext(localizedEntityMap,
            localizedIndices.textToUuidLookup(), localizedIndices.inboundIndex(), includeProperties,
            propertyFilter, relationFilter, isolatedStack, isolatedCache, mode);

        EntityGraphNode node = buildGraphNode(entry.getKey(), localizedCtx);
        result.put(entity.identifier(), node);
      }
    }

    return result;
  }

  /// Loads and builds entity graphs for multiple entity IDs in a single batch
  /// operation.
  @Transactional(readOnly = true)
  public Map<String, EntityGraphNode> loadAndBuildEntityGraphs(List<UUID> entityIds, int depth,
      boolean includeProperties, Set<String> relationFilter, Set<String> propertyFilter,
      EntityGraphTraversalMode mode) {

    if (entityIds == null || entityIds.isEmpty()) {
      return Map.of();
    }

    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);
    Map<UUID, Entity> entityGraphs = entityGraphRepositoryPort.findEntityGraphBatch(entityIds,
        effectiveDepth, includeProperties, mode);

    if (entityGraphs == null || entityGraphs.isEmpty()) {
      return Map.of();
    }

    IndexBundle globalIndices = buildIndices(entityGraphs, mode);
    Map<String, EntityGraphNode> result = new HashMap<>();

    for (Map.Entry<UUID, Entity> entry : entityGraphs.entrySet()) {
      Entity entity = entry.getValue();
      if (entity != null) {
        Set<UUID> reachableFootprint = computeReachableSubGraph(entry.getKey(), entityGraphs,
            globalIndices, effectiveDepth, mode);

        Map<UUID, Entity> localizedEntityMap = entityGraphs.entrySet().stream()
            .filter(e -> reachableFootprint.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        IndexBundle localizedIndices = buildIndices(localizedEntityMap, mode);

        Set<String> isolatedStack = new HashSet<>();
        Map<String, EntityGraphNode> isolatedCache = new HashMap<>();

        GraphTraversalContext localizedCtx = new GraphTraversalContext(localizedEntityMap,
            localizedIndices.textToUuidLookup(), localizedIndices.inboundIndex(), includeProperties,
            propertyFilter, relationFilter, isolatedStack, isolatedCache, mode);

        EntityGraphNode node = buildGraphNode(entry.getKey(), localizedCtx);
        result.put(entity.identifier(), node);
      }
    }

    return result;
  }

  /// Builds entity graphs for multiple entity string identifiers within a
  /// template.
  @Transactional(readOnly = true)
  public Map<String, EntityGraphNode> getBatchEntityGraphsByIdentifiers(String templateIdentifier,
      List<String> entityIdentifiers, int depth, boolean includeProperties,
      Set<String> relationFilter, Set<String> propertyFilter, EntityGraphTraversalMode mode) {

    if (entityIdentifiers == null || entityIdentifiers.isEmpty()) {
      return Map.of();
    }

    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);

    List<UUID> entityUuids = new ArrayList<>();
    for (String identifier : entityIdentifiers) {
      entityRepositoryPort.findByTemplateIdentifierAndIdentifier(templateIdentifier, identifier)
          .ifPresent(entity -> entityUuids.add(entity.id()));
    }

    if (entityUuids.isEmpty()) {
      return Map.of();
    }

    Map<UUID, Entity> entityGraphs = entityGraphRepositoryPort.findEntityGraphBatch(entityUuids,
        effectiveDepth, includeProperties, mode);

    if (entityGraphs == null || entityGraphs.isEmpty()) {
      return Map.of();
    }

    IndexBundle globalIndices = buildIndices(entityGraphs, mode);
    Map<String, EntityGraphNode> result = new HashMap<>();

    for (String identifier : entityIdentifiers) {
      UUID entityUuid = null;
      for (Map.Entry<UUID, Entity> entry : entityGraphs.entrySet()) {
        if (entry.getValue().identifier().equals(identifier)) {
          entityUuid = entry.getKey();
          break;
        }
      }

      if (entityUuid != null) {
        Set<UUID> reachableFootprint = computeReachableSubGraph(entityUuid, entityGraphs,
            globalIndices, effectiveDepth, mode);

        Map<UUID, Entity> localizedEntityMap = entityGraphs.entrySet().stream()
            .filter(e -> reachableFootprint.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        IndexBundle localizedIndices = buildIndices(localizedEntityMap, mode);

        Set<String> isolatedStack = new HashSet<>();
        Map<String, EntityGraphNode> isolatedCache = new HashMap<>();

        GraphTraversalContext localizedCtx = new GraphTraversalContext(localizedEntityMap,
            localizedIndices.textToUuidLookup(), localizedIndices.inboundIndex(), includeProperties,
            propertyFilter, relationFilter, isolatedStack, isolatedCache, mode);

        EntityGraphNode node = buildGraphNode(entityUuid, localizedCtx);
        result.put(identifier, node);
      }
    }

    return result;
  }

  private EntityGraphNode buildGraphNode(UUID entityUuid, GraphTraversalContext ctx) {
    Entity entity = ctx.entityMap().get(entityUuid);

    var nodeIdDisplay = entityUuid != null ? entityUuid.toString() : "null-entity-";
    if (entity == null) {
      return new EntityGraphNode(nodeIdDisplay, nodeIdDisplay, nodeIdDisplay, List.of(), List.of(),
          List.of());
    }

    var nodeId = entity.id().toString();

    if (ctx.activeStack().contains(nodeId)) {
      List<Property> stubProperties = resolveProperties(entity, ctx.includeProperties(),
          ctx.propertyFilter());
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          stubProperties, List.of(), List.of());
    }

    if (ctx.memoCache().containsKey(nodeId)) {
      return ctx.memoCache().get(nodeId);
    }

    ctx.activeStack().add(nodeId);

    List<EntityGraphRelation> outboundRelations = entity.relations().stream()
        .filter(relation -> ctx.relationFilter().isEmpty()
            || ctx.relationFilter().contains(relation.name()))
        .map(relation -> new EntityGraphRelation(relation.name(),
            relation.targetEntityIdentifiers().stream().map(targetId -> {
              UUID targetUuid = ctx.textToUuidLookup()
                  .get(new EntityCompositeKey(relation.targetTemplateIdentifier(), targetId));
              if (targetUuid == null)
                return null;

              return buildGraphNode(targetUuid, ctx);
            }).filter(Objects::nonNull).toList()))
        .filter(rel -> !rel.targets().isEmpty()).toList();

    List<EntityGraphRelation> inboundRelations = buildRelationsAsTargetFromIndex(
        entity.identifier(), ctx);

    List<Property> properties = resolveProperties(entity, ctx.includeProperties(),
        ctx.propertyFilter());

    EntityGraphNode completedNode = new EntityGraphNode(entity.templateIdentifier(),
        entity.identifier(), entity.name(), properties, outboundRelations, inboundRelations);

    ctx.memoCache().put(nodeId, completedNode);
    ctx.activeStack().remove(nodeId);

    return completedNode;
  }

  private List<EntityGraphRelation> buildRelationsAsTargetFromIndex(String targetIdentifier,
      GraphTraversalContext ctx) {
    if (ctx.mode() != EntityGraphTraversalMode.BIDIRECTIONAL
        && ctx.mode() != EntityGraphTraversalMode.DIRECT_LINEAGE) {
      return List.of();
    }

    String normalizedTargetIdentifier = targetIdentifier == null
        ? ""
        : targetIdentifier.trim().toLowerCase();
    Map<String, List<UUID>> sourcesByRelationName = ctx.inboundIndex()
        .getOrDefault(normalizedTargetIdentifier, Map.of());

    if (sourcesByRelationName.isEmpty()) {
      return List.of();
    }

    return sourcesByRelationName.entrySet().stream()
        .filter(e -> ctx.relationFilter().isEmpty() || ctx.relationFilter().contains(e.getKey()))
        .map(e -> {
          List<EntityGraphNode> targets = e.getValue().stream()
              .map(sourceUuid -> buildGraphNode(sourceUuid, ctx)).toList();
          return new EntityGraphRelation(e.getKey(), targets);
        }).toList();
  }

  private IndexBundle buildIndices(Map<UUID, Entity> entityMap, EntityGraphTraversalMode mode) {
    Map<EntityCompositeKey, UUID> textToUuidLookup = new HashMap<>();
    Map<String, Map<String, List<UUID>>> inboundIndex = new HashMap<>();

    for (Map.Entry<UUID, Entity> entry : entityMap.entrySet()) {
      UUID sourceUuid = entry.getKey();
      Entity entity = entry.getValue();
      if (entity == null)
        continue;

      textToUuidLookup.put(new EntityCompositeKey(entity.templateIdentifier(), entity.identifier()),
          sourceUuid);

      if (mode == EntityGraphTraversalMode.BIDIRECTIONAL
          || mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
        buildInboundIndexForEntity(entity, sourceUuid, inboundIndex);
      }
    }
    return new IndexBundle(textToUuidLookup, inboundIndex);
  }

  private void buildInboundIndexForEntity(Entity entity, UUID sourceUuid,
      Map<String, Map<String, List<UUID>>> inboundIndex) {
    for (Relation relation : entity.relations()) {
      for (String targetId : relation.targetEntityIdentifiers()) {
        if (targetId == null)
          continue;
        String normalizedTargetId = targetId.trim().toLowerCase();
        inboundIndex.computeIfAbsent(normalizedTargetId, k -> new HashMap<>())
            .computeIfAbsent(relation.name(), k -> new ArrayList<>()).add(sourceUuid);
      }
    }
  }

  private List<Property> resolveProperties(Entity entity, boolean includeProperties,
      Set<String> propertyFilter) {
    if (!includeProperties)
      return List.of();
    if (propertyFilter.isEmpty())
      return entity.properties();
    return entity.properties().stream().filter(p -> propertyFilter.contains(p.name())).toList();
  }

  private static record ReachableState(UUID id, String flow) {
  }

  /// Computes a precise sub-graph footprint matching the recursive database query
  /// logic.
  private Set<UUID> computeReachableSubGraph(UUID rootId, Map<UUID, Entity> entityMap,
      IndexBundle globalIndices, int maxDepth, EntityGraphTraversalMode mode) {

    Set<ReachableState> visited = new HashSet<>();
    Set<ReachableState> currentLevel = new HashSet<>();

    // Replicate SQL CTE Anchor Members
    if (mode == EntityGraphTraversalMode.OUTBOUND_ONLY
        || mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
      ReachableState anchor = new ReachableState(rootId, "OUTBOUND");
      visited.add(anchor);
      currentLevel.add(anchor);
    }
    if (mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
      ReachableState anchor = new ReachableState(rootId, "INBOUND");
      if (visited.add(anchor)) {
        currentLevel.add(anchor);
      }
    }
    if (mode == EntityGraphTraversalMode.BIDIRECTIONAL) {
      ReachableState anchor = new ReachableState(rootId, "ANY");
      visited.add(anchor);
      currentLevel.add(anchor);
    }

    // Level-by-level propagation matching depth limits
    for (int d = 0; d < maxDepth; d++) {
      Set<ReachableState> nextLevel = new HashSet<>();
      for (ReachableState state : currentLevel) {
        Entity entity = entityMap.get(state.id());
        if (entity == null)
          continue;

        // Propigate Outbound Paths
        if ("OUTBOUND".equals(state.flow()) || "ANY".equals(state.flow())) {
          for (Relation rel : entity.relations()) {
            for (String targetId : rel.targetEntityIdentifiers()) {
              UUID targetUuid = globalIndices.textToUuidLookup()
                  .get(new EntityCompositeKey(rel.targetTemplateIdentifier(), targetId));
              if (targetUuid != null && entityMap.containsKey(targetUuid)) {
                ReachableState nextState = new ReachableState(targetUuid, state.flow());
                if (visited.add(nextState)) {
                  nextLevel.add(nextState);
                }
              }
            }
          }
        }

        // Propigate Inbound Paths
        if ("INBOUND".equals(state.flow()) || "ANY".equals(state.flow())) {
          String normalizedId = entity.identifier() == null
              ? ""
              : entity.identifier().trim().toLowerCase();
          Map<String, List<UUID>> sourcesByRelation = globalIndices.inboundIndex()
              .getOrDefault(normalizedId, Map.of());
          for (List<UUID> sources : sourcesByRelation.values()) {
            for (UUID sourceUuid : sources) {
              if (entityMap.containsKey(sourceUuid)) {
                ReachableState nextState = new ReachableState(sourceUuid, state.flow());
                if (visited.add(nextState)) {
                  nextLevel.add(nextState);
                }
              }
            }
          }
        }
      }
      if (nextLevel.isEmpty())
        break;
      currentLevel = nextLevel;
    }

    return visited.stream().map(ReachableState::id).collect(Collectors.toSet());
  }

  private static record IndexBundle(Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex) {
  }

  private static record GraphTraversalContext(Map<UUID, Entity> entityMap,
      Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex, boolean includeProperties,
      Set<String> propertyFilter, Set<String> relationFilter, Set<String> activeStack,
      Map<String, EntityGraphNode> memoCache, EntityGraphTraversalMode mode) {
  }

  private static record EntityCompositeKey(String templateIdentifier, String identifier) {
    public EntityCompositeKey {
      templateIdentifier = templateIdentifier == null
          ? ""
          : templateIdentifier.trim().toLowerCase();
      identifier = identifier == null ? "" : identifier.trim().toLowerCase();
    }
  }
}
