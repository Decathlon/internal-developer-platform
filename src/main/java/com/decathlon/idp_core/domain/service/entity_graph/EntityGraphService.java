package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

    // 1. Resolve root entity
    Entity rootEntity = entityRepositoryPort
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

    // 2. Load the graph footprint via optimized DB calls
    Map<UUID, Entity> entityMap = entityGraphRepositoryPort.findEntityGraph(rootEntity.id(),
        effectiveDepth, includeProperties, mode);

    if (entityMap == null || entityMap.isEmpty()) {
      return new EntityGraphNode(rootEntity.templateIdentifier(), rootEntity.identifier(),
          rootEntity.name(), List.of(), List.of(), List.of());
    }

    // 3. Pre-computation Layer
    IndexBundle indices = buildIndices(entityMap, mode);

    // Context tracking for this execution tree
    Set<String> activeStack = new HashSet<>();
    Map<String, EntityGraphNode> memoCache = new HashMap<>();

    GraphTraversalContext ctx = new GraphTraversalContext(entityMap, indices.textToUuidLookup(),
        indices.inboundIndex(), includeProperties, propertyFilter, relationFilter, activeStack,
        memoCache, mode);

    // 4. Trigger recursive tree mapping (O(N) performance, heap-safe)
    return buildGraphNode(rootEntity.id(), ctx);
  }

  private EntityGraphNode buildGraphNode(UUID entityUuid, GraphTraversalContext ctx) {
    Entity entity = ctx.entityMap().get(entityUuid);

    var nodeIdDisplay = entityUuid != null ? entityUuid.toString() : "null-entity-";
    if (entity == null) {
      return new EntityGraphNode(nodeIdDisplay, nodeIdDisplay, nodeIdDisplay, List.of(), List.of(),
          List.of());
    }

    var nodeId = entity.id().toString();

    // GUARD 1: If the node is currently in active processing up our current line,
    // we hit a cyclic loop closure. Break instantly with a stub to prevent infinite
    // stack overflow.
    if (ctx.activeStack().contains(nodeId)) {
      List<Property> stubProperties = resolveProperties(entity, ctx.includeProperties(),
          ctx.propertyFilter());
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          stubProperties, List.of(), List.of());
    }

    // GUARD 2: MEMOIZATION CHECK. If this node has already been built down an
    // alternate path,
    // return the pre-existing reference instantly. This prevents the exponential
    // path explosion.
    if (ctx.memoCache().containsKey(nodeId)) {
      return ctx.memoCache().get(nodeId);
    }

    // Push to active processing stack
    ctx.activeStack().add(nodeId);

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
    ctx.memoCache().put(nodeId, completedNode);
    ctx.activeStack().remove(nodeId);

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

  private static record IndexBundle(Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex) {
  }

  private static record GraphTraversalContext(Map<UUID, Entity> entityMap,
      Map<EntityCompositeKey, UUID> textToUuidLookup,
      Map<String, Map<String, List<UUID>>> inboundIndex, boolean includeProperties,
      Set<String> propertyFilter, Set<String> relationFilter, Set<String> activeStack,
      Map<String, EntityGraphNode> memoCache, // High-speed in-memory reuse cache
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
