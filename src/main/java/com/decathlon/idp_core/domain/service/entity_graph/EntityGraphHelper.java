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

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.entity_graph.FlowDirection;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class EntityGraphHelper {

  /// Bulk graph-building logic. Avoid self-proxy transactional calls.
  ///
  /// **Design decision:** Extracted as a private non-transactional helper so
  /// callers that already own a transaction can reuse the logic directly without
  /// triggering a new transaction via the Spring proxy.
  ///
  /// @param entityIds UUIDs of the root entities to build graphs for
  /// @param includeProperties whether to include property data in the nodes
  /// @param relationFilter relation names to include; empty means all
  /// @param propertyFilter property names to include; empty means all
  /// @param mode traversal direction (OUTBOUND, BIDIRECTIONAL, etc.)
  /// @param depth traversal depth
  /// @return map of entity identifier to its fully resolved EntityGraphNode
  @SuppressWarnings("null")
  public Map<UUID, EntityGraphNode> buildGraphNodesForEntityIds(Map<UUID, Entity> entityGraphs,
      boolean includeProperties, Set<String> relationFilter, Set<String> propertyFilter,
      EntityGraphTraversalMode mode, int depth) {

    if (entityGraphs == null || entityGraphs.isEmpty()) {
      return Map.of();
    }

    IndexBundle globalIndices = buildIndices(entityGraphs, mode);
    Map<UUID, EntityGraphNode> result = new HashMap<>();

    for (Map.Entry<UUID, Entity> entry : entityGraphs.entrySet()) {
      Entity entity = entry.getValue();
      if (entity != null) {

        Set<UUID> reachableFootprint = computeReachableSubGraph(entry.getKey(), entityGraphs,
            globalIndices, depth, mode);

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
        result.put(entry.getKey(), node);
      }
    }

    return result;
  }

  /// Traces an in-memory reachable footprint starting from a root entity,
  /// replicating recursive CTE logic.
  ///
  /// This executes a breadth-first search (BFS) level-by-level propagation to
  /// find
  /// all reachable nodes, ensuring that the in-memory tree building only
  /// processes
  /// nodes connected to the root.
  ///
  /// @param rootId the starting entity UUID
  /// @param entityMap the raw loaded entity registry
  /// @param globalIndices fast lookup maps containing keys and inbound relations
  /// @param maxDepth depth limit for propagation
  /// @param mode the active traversal mode
  /// @return a set of UUIDs belonging to the reachable footprint
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

  /// Establishes the initial anchor states for BFS propagation based on the
  /// traversal mode.
  ///
  /// Mimics the anchor member execution of a recursive SQL query.
  ///
  /// @param rootId the UUID of the starting entity
  /// @param mode the active traversal direction mode
  /// @param visited the tracking set to record visited states
  /// @return the initialized set of states for the first level of BFS propagation
  private Set<ReachableState> initializeAnchorStates(UUID rootId, EntityGraphTraversalMode mode,
      Set<ReachableState> visited) {
    Set<ReachableState> currentLevel = new HashSet<>();

    if (mode == EntityGraphTraversalMode.OUTBOUND_ONLY
        || mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
      ReachableState anchor = new ReachableState(rootId, FlowDirection.OUTBOUND);
      visited.add(anchor);
      currentLevel.add(anchor);
    }
    if (mode == EntityGraphTraversalMode.DIRECT_LINEAGE) {
      ReachableState anchor = new ReachableState(rootId, FlowDirection.INBOUND);
      if (visited.add(anchor)) {
        currentLevel.add(anchor);
      }
    }
    if (mode == EntityGraphTraversalMode.BIDIRECTIONAL) {
      ReachableState anchor = new ReachableState(rootId, FlowDirection.ANY);
      visited.add(anchor);
      currentLevel.add(anchor);
    }

    return currentLevel;
  }

  /// Propagates outbound and inbound relationship lines for all nodes in the
  /// current BFS layer.
  ///
  /// @param currentLevel active states to expand
  /// @param entityMap raw mapping database records
  /// @param globalIndices pre-computed reverse mapping lookups
  /// @param visited tracking set to avoid re-evaluating visited states
  /// @return the set of state steps detected for the next depth level
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

  /// Discovers and queues adjacent downstream targets connected through outbound
  /// relations.
  ///
  /// Only executes if the current state allows outbound flows (`OUTBOUND` or
  /// `ANY`).
  ///
  /// @param state the current traversal position and flow direction
  /// @param entity the model metadata of the current node
  /// @param entityMap raw target lookup reference
  /// @param globalIndices lookup indices to resolve template names
  /// @param visited state memory to prevent circular steps
  /// @param nextLevel output set to collect newly discovered states
  private void propagateOutboundPaths(ReachableState state, Entity entity,
      Map<UUID, Entity> entityMap, IndexBundle globalIndices, Set<ReachableState> visited,
      Set<ReachableState> nextLevel) {
    if (!FlowDirection.OUTBOUND.equals(state.flow()) && !FlowDirection.ANY.equals(state.flow())) {
      return;
    }

    for (Relation rel : entity.relations()) {
      for (String targetId : rel.targetEntityIdentifiers()) {
        UUID targetUuid = globalIndices.textToUuidLookup()
            .get(new EntityCompositeKey(rel.targetTemplateIdentifier(), targetId));
        if (targetUuid != null && entityMap.containsKey(targetUuid)) {
          ReachableState nextState = new ReachableState(targetUuid, FlowDirection.OUTBOUND);
          if (visited.add(nextState)) {
            nextLevel.add(nextState);
          }
        }
      }
    }
  }

  /// Discovers and queues adjacent upstream sources connected through inbound
  /// relations.
  ///
  /// Resolves incoming references via the pre-built global inbound index. Only
  /// executes if the current state allows inbound flows (`INBOUND` or `ANY`).
  ///
  /// @param state the current traversal position and flow direction
  /// @param entity the model metadata of the current node
  /// @param entityMap raw target lookup reference
  /// @param globalIndices lookup indices to resolve incoming references
  /// @param visited state memory to prevent circular steps
  /// @param nextLevel output set to collect newly discovered states
  private void propagateInboundPaths(ReachableState state, Entity entity,
      Map<UUID, Entity> entityMap, IndexBundle globalIndices, Set<ReachableState> visited,
      Set<ReachableState> nextLevel) {
    if (!FlowDirection.INBOUND.equals(state.flow()) && !FlowDirection.ANY.equals(state.flow())) {
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
          ReachableState nextState = new ReachableState(sourceUuid, FlowDirection.INBOUND);
          if (visited.add(nextState)) {
            nextLevel.add(nextState);
          }
        }
      }
    }
  }

  /// Recursively constructs an individual [EntityGraphNode] using a Depth-First
  /// Search (DFS) strategy.
  ///
  /// This method implements two critical runtime protections:
  ///
  /// 1. **Cycle Detection (Active Stack):** If a node is currently in
  /// `activeStack`, a circular relationship loop (e.g., A -> B -> A) has been
  /// met. It returns a flat stub to prevent infinite stack overflow.
  /// 2. **Path Memoization (Cache Check):** If a node has already been built
  /// along
  /// a different path, its pre-built reference is returned instantly from
  /// `memoCache`, eliminating exponential $O(2^{\text{depth}})$ path traversal.
  ///
  /// @param entityUuid the UUID of the target entity to resolve
  /// @param ctx the active traversal configuration, indices, processing
  /// stack, and memoization cache
  /// @return a fully populated, nested [EntityGraphNode] tree representing the
  /// entity and its branches
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
    // alternate path, return the pre-existing reference instantly.
    // This prevents the exponential path explosion.
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

  /// Looks up and builds nested inbound relationships where the current entity
  /// acts as a target.
  ///
  /// This is only evaluated if the current mode supports incoming relationships
  /// (`BIDIRECTIONAL` or `DIRECT_LINEAGE`). Uses the pre-compiled localized
  /// inbound map to achieve fast O(1) lookups.
  ///
  /// @param targetIdentifier the business identifier of the entity acting as a
  /// relationship target
  /// @param ctx the active traversal context
  /// @return a list of inbound [EntityGraphRelation] nodes pointing to this
  /// target
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

  /// Indexes the raw entity map to support fast O(1) lookups during graph
  /// resolution.
  ///
  /// Generates standard composite key lookups (Template + Business Identifier ->
  /// UUID) and registers inbound links if directionality configurations permit.
  ///
  /// @param entityMap flat database records to index
  /// @param mode the active traversal direction mode
  /// @return an [IndexBundle] containing text-to-UUID and inbound mapping indexes
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

  /// Populates the inbound reference index for a specific entity's outgoing
  /// relations.
  ///
  /// Inverts relationship mapping so that target entities can look up their
  /// source
  /// origins in O(1) time.
  ///
  /// @param entity the source entity whose outgoing relations are scanned
  /// @param sourceUuid the database UUID of the source entity
  /// @param inboundIndex the active reverse lookup index under construction
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

  /// Resolves and filters an entity's properties based on the request settings.
  ///
  /// @param entity the source entity record
  /// @param includeProperties whether to fetch property fields
  /// @param propertyFilter set of allowed property names (empty = allow all)
  /// @return a list of filtered [Property] records
  private List<Property> resolveProperties(Entity entity, boolean includeProperties,
      Set<String> propertyFilter) {
    if (!includeProperties)
      return List.of();
    if (propertyFilter.isEmpty())
      return entity.properties();
    return entity.properties().stream().filter(p -> propertyFilter.contains(p.name())).toList();
  }

  private static record ReachableState(UUID id, FlowDirection flow) {
  }

  private static record IndexBundle(Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex) {
  }

  private static record GraphTraversalContext(Map<UUID, Entity> entityMap,
      Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex, boolean includeProperties,
      Set<String> propertyFilter, Set<String> relationFilter, Set<UUID> activeStack,
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
