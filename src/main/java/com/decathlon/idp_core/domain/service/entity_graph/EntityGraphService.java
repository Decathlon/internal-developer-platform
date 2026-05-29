package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityCompositeKey;
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
/// Resolves an entity's outbound and inbound relations recursively up to a configurable depth,
/// returning a tree of [EntityGraphNode] records containing summary information
/// for each connected entity.
///
/// **Business purpose:**
/// - Visualizing entity dependency graphs in the catalog UI
/// - Understanding relationship chains (e.g., service → database → infrastructure)
/// - Providing hierarchical views for impact analysis and change propagation
///
/// **Design decisions:**
/// - Uses depth-limited traversal to prevent unbounded recursion
/// - Optimized with recursive CTE and batch loading to minimize database queries
/// - A per-request `visitedNodeIds` set prevents exponential recursion: without it,
///   inbound relation scanning would re-expand already-visited nodes at every depth
///   level, producing O(2^depth) calls even for small graphs (OOM at depth ≥ 10).
/// - Relation and property filtering are domain concerns applied during graph construction,
///   so that callers (e.g. the REST controller) receive a graph that already respects
///   the requested scope instead of carrying unnecessary data to the Infrastructure layer.
@Service
@RequiredArgsConstructor
public class EntityGraphService {

  private static final int MAX_DEPTH = 10;

  private final EntityRepositoryPort entityRepositoryPort;
  private final EntityGraphRepositoryPort entityGraphRepositoryPort;
  private final EntityTemplateValidationService entityTemplateValidationService;

  /// Builds the relationship graph for an entity starting from its composite key.
  ///
  /// Relation and property filtering are applied here in the domain layer so that
  /// callers receive a correctly scoped graph without needing to know about
  /// filtering
  /// logic.
  ///
  /// @param templateIdentifier the template identifier of the root entity
  /// @param entityIdentifier the business identifier of the root entity
  /// @param depth the maximum traversal depth (clamped to [1, MAX_DEPTH])
  /// @param includeProperties when true, each graph node carries the entity's
  /// full property list (subject to propertyFilter)
  /// @param relationFilter when non-empty, only relations whose name is in this
  /// set are included in the graph; an empty set means no filter — all relations
  /// are included
  /// @param propertyFilter when non-empty, each node's property list is
  /// restricted to properties whose name is in this set; an empty set means no
  /// filter — all properties are included
  /// @return the root graph node with all resolved (and filtered) relations
  /// @throws EntityNotFoundException when no entity matches the given identifiers
  @Transactional(readOnly = true)
  public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier,
      int depth, boolean includeProperties, Set<String> relationFilter,
      Set<String> propertyFilter) {
    int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);

    entityTemplateValidationService.validateTemplateExists(templateIdentifier);

    Entity rootEntity = entityRepositoryPort
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

    Map<EntityCompositeKey, Entity> entityMap = entityGraphRepositoryPort
        .findEntityGraph(templateIdentifier, entityIdentifier, effectiveDepth, includeProperties);

    EntityCompositeKey rootKey = new EntityCompositeKey(rootEntity.templateIdentifier(),
        rootEntity.identifier());

    // One shared visited set per request — each node is fully expanded at most
    // once,
    // preventing O(2^depth) recursion from mutual outbound/inbound re-expansion.
    Set<String> visitedNodeIds = new HashSet<>();

    return buildGraphNode(rootKey, entityMap, effectiveDepth, includeProperties, relationFilter,
        propertyFilter, visitedNodeIds);
  }

  /// Builds a graph node from a pre-loaded entity map (no database calls).
  ///
  /// [visitedNodeIds] tracks nodes that have already been fully built in this
  /// traversal.
  /// When a node is encountered again (cycle or shared reference), a stub leaf is
  /// returned
  /// immediately to cut the recursion — preventing the exponential blowup that
  /// arises from
  /// inbound scanning re-expanding the same nodes at every depth level.
  private EntityGraphNode buildGraphNode(EntityCompositeKey key,
      Map<EntityCompositeKey, Entity> entityMap, int remainingDepth, boolean includeProperties,
      Set<String> relationFilter, Set<String> propertyFilter, Set<String> visitedNodeIds) {
    Entity entity = entityMap.get(key);
    if (entity == null) {
      return new EntityGraphNode(key.templateIdentifier(), key.identifier(), key.identifier(),
          List.of(), List.of(), List.of());
    }

    // Guard: return a stub leaf if this node was already fully built in another
    // branch.
    // This breaks both directed cycles (A→B→A) and shared references (A→B, C→B).
    // Properties are still included so data is not silently dropped for shared
    // nodes.
    var nodeId = entity.templateIdentifier() + ":" + entity.identifier();
    if (!visitedNodeIds.add(nodeId)) {
      List<Property> stubProperties = resolveProperties(entity, includeProperties, propertyFilter);
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          stubProperties, List.of(), List.of());
    }

    // Depth exhausted — return a leaf with no relations but still carry properties
    // so the deepest reachable entities expose their data when include_data=true.
    if (remainingDepth <= 0) {
      List<Property> leafProperties = resolveProperties(entity, includeProperties, propertyFilter);
      return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
          leafProperties, List.of(), List.of());
    }

    List<EntityGraphRelation> outboundRelations = entity.relations().stream()
        .filter(relation -> relationFilter.isEmpty() || relationFilter.contains(relation.name()))
        .map(relation -> new EntityGraphRelation(relation.name(),
            relation.targetEntityIdentifiers().stream()
                .map(targetId -> buildGraphNode(findKeyByIdentifier(targetId, entityMap), entityMap,
                    remainingDepth - 1, includeProperties, relationFilter, propertyFilter,
                    visitedNodeIds))
                .toList()))
        .toList();

    List<EntityGraphRelation> inboundRelations = buildRelationsAsTargetFromMap(entity.identifier(),
        entityMap, remainingDepth - 1, includeProperties, relationFilter, propertyFilter,
        visitedNodeIds);

    List<Property> properties = resolveProperties(entity, includeProperties, propertyFilter);
    return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
        properties, outboundRelations, inboundRelations);
  }

  /// Looks up a composite key from the map by identifier alone.
  /// Falls back to a synthetic key if no match is found (entity not in graph).
  private EntityCompositeKey findKeyByIdentifier(String identifier,
      Map<EntityCompositeKey, Entity> entityMap) {
    return entityMap.keySet().stream().filter(k -> k.identifier().equals(identifier)).findFirst()
        .orElse(new EntityCompositeKey("", identifier));
  }

  /// Builds incoming relations (where this entity is the target) from the
  /// pre-loaded entity map.
  /// Passes [visitedNodeIds] through so that source nodes already expanded
  /// elsewhere are not
  /// re-expanded here, preventing the mutual recursion that causes OOM at high
  /// depths.
  private List<EntityGraphRelation> buildRelationsAsTargetFromMap(String targetIdentifier,
      Map<EntityCompositeKey, Entity> entityMap, int remainingDepth, boolean includeProperties,
      Set<String> relationFilter, Set<String> propertyFilter, Set<String> visitedNodeIds) {
    Map<String, List<EntityCompositeKey>> sourcesByRelationName = new HashMap<>();

    for (Map.Entry<EntityCompositeKey, Entity> entry : entityMap.entrySet()) {
      Entity sourceEntity = entry.getValue();
      for (Relation relation : sourceEntity.relations()) {
        if (relation.targetEntityIdentifiers().contains(targetIdentifier)
            && (relationFilter.isEmpty() || relationFilter.contains(relation.name()))) {
          sourcesByRelationName.computeIfAbsent(relation.name(), k -> new ArrayList<>())
              .add(entry.getKey());
        }
      }
    }

    return sourcesByRelationName.entrySet().stream()
        .map(e -> new EntityGraphRelation(e.getKey(),
            e.getValue().stream()
                .map(sourceKey -> buildGraphNode(sourceKey, entityMap, remainingDepth,
                    includeProperties, relationFilter, propertyFilter, visitedNodeIds))
                .toList()))
        .toList();
  }

  /// Returns the entity's properties filtered by [propertyFilter] when active,
  /// or an empty list when [includeProperties] is false.
  private List<Property> resolveProperties(Entity entity, boolean includeProperties,
      Set<String> propertyFilter) {
    if (!includeProperties) {
      return List.of();
    }
    if (propertyFilter.isEmpty()) {
      return entity.properties();
    }
    return entity.properties().stream().filter(p -> propertyFilter.contains(p.name())).toList();
  }
}
