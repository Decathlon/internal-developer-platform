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

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityGraphHelper {


  private static final String FLOW_OUTBOUND = "OUTBOUND";
  private static final String FLOW_INBOUND = "INBOUND";
  private static final String FLOW_ANY = "ANY";



  private final EntityGraphRepositoryPort entityGraphRepositoryPort;
  /// Bulk graph-building logic used by both. 
  /// Avoid self-proxy transactional calls.
  ///
  /// **Design decision:** Extracted as a private non-transactional helper so
  /// callers that already own a transaction can reuse the logic directly without
  /// triggering a new transaction via the Spring proxy.
  ///
  /// @param entityIds UUIDs of the root entities to build graphs for
  /// @param depth traversal depth clamped to [1, MAX_DEPTH]
  /// @param includeProperties whether to include property data in the nodes
  /// @param relationFilter relation names to include; empty means all
  /// @param propertyFilter property names to include; empty means all
  /// @param mode traversal direction (OUTBOUND_ONLY, BIDIRECTIONAL, etc.)
  /// @return map of entity identifier to its fully resolved EntityGraphNode
  @SuppressWarnings("null")
  public Map<UUID, EntityGraphNode> buildGraphNodesForEntityIds(Map<UUID, Entity> entityGraphs, int depth,
      boolean includeProperties, Set<String> relationFilter, Set<String> propertyFilter,
      EntityGraphTraversalMode mode, int effectiveDepth) {

    if (entityGraphs == null || entityGraphs.isEmpty()) {
      return Map.of();
    }

    IndexBundle globalIndices = buildIndices(entityGraphs, mode);
    Map<UUID, EntityGraphNode> result = new HashMap<>();

    for (Map.Entry<UUID, Entity> entry : entityGraphs.entrySet()) {
      Entity entity = entry.getValue();
      if (entity != null) {
        
        Set<UUID> reachableFootprint = computeReachableSubGraph(entry.getKey(), entityGraphs,
            globalIndices, effectiveDepth, mode);

        Map<UUID, Entity> localizedEntityMap = entityGraphs.entrySet().stream()
            .filter(e -> reachableFootprint.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        IndexBundle localizedIndices = buildIndices(localizedEntityMap, mode);

        Set<UUID> isolatedStack = new HashSet<>();
        Map<UUID, EntityGraphNode> isolatedCache = new HashMap<>();

        GraphTraversalContext localizedCtx = new GraphTraversalContext(localizedEntityMap,
            localizedIndices.textToUuidLookup(), localizedIndices.inboundIndex(), includeProperties,
            propertyFilter, relationFilter, isolatedStack, isolatedCache, mode);

        EntityGraphNode node = buildGraphNode(entry.getKey(), localizedCtx);
        result.put(entity.id(), node);
      }
    }

    return result;
  }

  /// Computes a precise sub-graph footprint
  /// matching the recursive database query logic.
  @SuppressWarnings("null")
  private Set<UUID> computeReachableSubGraph(UUID rootId, Map<UUID, Entity> entityMap,
      IndexBundle globalIndices, int maxDepth, EntityGraphTraversalMode mode) {

    Set<ReachableState> visited = new HashSet<>();
    Set<ReachableState> currentLevel = initializeAnchorStates(rootId, mode, visited);

    // Level-by-level propagation matching depth limits
    for (int d = 0; d < maxDepth; d++) {
      Set<ReachableState> nextLevel = expandCurrentLevel(currentLevel, entityMap, globalIndices,
          visited);
      if (nextLevel.isEmpty())
        break;
      currentLevel = nextLevel;
    }

    return visited.stream().map(ReachableState::id).collect(Collectors.toSet());
  }

  /// Initializes anchor states based on traversal mode, replicating SQL CTE
  /// anchor members.
  private Set<ReachableState> initializeAnchorStates(UUID rootId,
      EntityGraphTraversalMode mode, Set<ReachableState> visited) {
    Set<ReachableState> currentLevel = new HashSet<>();

    if (mode == EntityGraphTraversalMode.OUTBOUND_ONLY
        || mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
      ReachableState anchor = new ReachableState(rootId, FLOW_OUTBOUND);
      visited.add(anchor);
      currentLevel.add(anchor);
    }
    if (mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
      ReachableState anchor = new ReachableState(rootId, FLOW_INBOUND);
      if (visited.add(anchor)) {
        currentLevel.add(anchor);
      }
    }
    if (mode == EntityGraphTraversalMode.BIDIRECTIONAL) {
      ReachableState anchor = new ReachableState(rootId, FLOW_ANY);
      visited.add(anchor);
      currentLevel.add(anchor);
    }

    return currentLevel;
  }

  /// Expands the current level of states to the next level by propagating
  /// outbound and inbound paths.
  private Set<ReachableState> expandCurrentLevel(Set<ReachableState> currentLevel,
      Map<UUID, Entity> entityMap, IndexBundle globalIndices, Set<ReachableState> visited) {
    Set<ReachableState> nextLevel = new HashSet<>();

    for (ReachableState state : currentLevel) {
      Entity entity = entityMap.get(state.id());
      if (entity == null)
        continue;

      propagateOutboundPaths(state, entity, entityMap, globalIndices, visited, nextLevel);
      propagateInboundPaths(state, entity, entityMap, globalIndices, visited, nextLevel);
    }

    return nextLevel;
  }

  /// Propagates outbound relationship paths to target entities.
  private void propagateOutboundPaths(ReachableState state, Entity entity,
      Map<UUID, Entity> entityMap, IndexBundle globalIndices, Set<ReachableState> visited,
      Set<ReachableState> nextLevel) {
    if (!FLOW_OUTBOUND.equals(state.flow()) && !FLOW_ANY.equals(state.flow())) {
      return;
    }

    for (Relation rel : entity.relations()) {
      for (String targetId : rel.targetEntityIdentifiers()) {
        UUID targetUuid = globalIndices.textToUuidLookup()
            .get(new EntityCompositeKey(rel.targetTemplateIdentifier(), targetId));
        if (targetUuid != null && entityMap.containsKey(targetUuid)) {
          ReachableState nextState = new ReachableState(targetUuid, FLOW_OUTBOUND);
          if (visited.add(nextState)) {
            nextLevel.add(nextState);
          }
        }
      }
    }
  }

  /// Propagates inbound relationship paths from source entities.
  private void propagateInboundPaths(ReachableState state, Entity entity,
      Map<UUID, Entity> entityMap, IndexBundle globalIndices, Set<ReachableState> visited,
      Set<ReachableState> nextLevel) {
    if (!FLOW_INBOUND.equals(state.flow()) && !FLOW_ANY.equals(state.flow())) {
      return;
    }

    String normalizedId = entity.identifier() == null
        ? ""
        : entity.identifier().trim().toLowerCase();
    Map<String, List<UUID>> sourcesByRelation = globalIndices.inboundIndex()
        .getOrDefault(normalizedId, Map.of());

    for (List<UUID> sources : sourcesByRelation.values()) {
      for (UUID sourceUuid : sources) {
        if (entityMap.containsKey(sourceUuid)) {
          ReachableState nextState = new ReachableState(sourceUuid, FLOW_INBOUND);
          if (visited.add(nextState)) {
            nextLevel.add(nextState);
          }
        }
      }
    }
  }

  private EntityGraphNode buildGraphNode(UUID entityUuid, GraphTraversalContext ctx) {
    Entity entity = ctx.entityMap().get(entityUuid);

    var nodeIdDisplay = entityUuid != null ? entityUuid.toString() : "null-entity-";
    if (entity == null) {
      return new EntityGraphNode(nodeIdDisplay, nodeIdDisplay, nodeIdDisplay, List.of(), List.of(),
          List.of());
    }

    // GUARD 1: If the node is currently in active processing up our current line,
    // we hit a cyclic loop closure. Break instantly with a stub to prevent infinite
    // stack overflow.
    if (ctx.activeStack().contains(entityUuid)) {
      List<Property> stubProperties = resolveProperties(entity, ctx.includeProperties(),
          ctx.propertyFilter());
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          stubProperties, List.of(), List.of());
    }

    // GUARD 2: MEMOIZATION CHECK. If this node has already been built down an
    // alternate path,
    // return the pre-existing reference instantly. This prevents the exponential
    // path explosion.
    if (ctx.memoCache().containsKey(entityUuid)) {
      return ctx.memoCache().get(entityUuid);
    }

    // Push to active processing stack
    ctx.activeStack().add(entityUuid);

    // Process outbound relationships
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

    // Process inbound relationships
    List<EntityGraphRelation> inboundRelations = buildRelationsAsTargetFromIndex(
        entity.identifier(), ctx);

    List<Property> properties = resolveProperties(entity, ctx.includeProperties(),
        ctx.propertyFilter());

    // Assemble the complete node object
    EntityGraphNode completedNode = new EntityGraphNode(entity.templateIdentifier(),
        entity.identifier(), entity.name(), properties, outboundRelations, inboundRelations);

    // Save to Cache and pop from active stack before returning
    ctx.memoCache().put(entityUuid, completedNode);
    ctx.activeStack().remove(entityUuid);

    return completedNode;
  }

  private List<EntityGraphRelation> buildRelationsAsTargetFromIndex(String targetIdentifier,
      GraphTraversalContext ctx) {
    // Include inbound relations for BIDIRECTIONAL and DIRECT_LINEAGE modes
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

  public IndexBundle buildIndices(Map<UUID, Entity> entityMap, EntityGraphTraversalMode mode) {
    Map<EntityCompositeKey, UUID> textToUuidLookup = new HashMap<>();
    Map<String, Map<String, List<UUID>>> inboundIndex = new HashMap<>();

    for (Map.Entry<UUID, Entity> entry : entityMap.entrySet()) {
      UUID sourceUuid = entry.getKey();
      Entity entity = entry.getValue();
      if (entity == null)
        continue;

      textToUuidLookup.put(new EntityCompositeKey(entity.templateIdentifier(), entity.identifier()),
          sourceUuid);

      // Build inbound index for BIDIRECTIONAL and DIRECT_LINEAGE modes
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
  private static record IndexBundle(Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex) {
  }

  private static record GraphTraversalContext(
      Map<UUID, Entity> entityMap,
      Map<EntityCompositeKey,UUID> textToUuidLookup,
      Map<String, Map<String,List<UUID>>> inboundIndex,
      boolean includeProperties,
      Set<String> propertyFilter,
      Set<String> relationFilter,
      Set<UUID> activeStack,
      Map<UUID, EntityGraphNode> memoCache, // High-speed in-memory reuse cache
      EntityGraphTraversalMode mode) {
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
