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
/// - The service always returns the full unfiltered graph tree. Relation name filtering
///   is a presentation concern applied by the mapper layer.
@Service
@RequiredArgsConstructor
public class EntityGraphService {

    private static final int MAX_DEPTH = 10;

    private final EntityRepositoryPort entityRepositoryPort;
    private final EntityGraphRepositoryPort entityGraphRepositoryPort;

    /// Builds the relationship graph for an entity starting from its composite key.
    ///
    /// @param templateIdentifier the template identifier of the root entity
    /// @param entityIdentifier   the business identifier of the root entity
    /// @param depth              the maximum traversal depth (clamped to [1, MAX_DEPTH])
    /// @param includeProperties  when true, each graph node carries the entity's full property list
    /// @return the root graph node with all resolved relations
    /// @throws EntityNotFoundException when no entity matches the given identifiers
    @Transactional(readOnly = true)
    public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier, int depth,
            boolean includeProperties) {
        int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);

        Entity rootEntity = entityRepositoryPort
                .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

        Map<EntityCompositeKey, Entity> entityMap = entityGraphRepositoryPort
                .findEntityGraph(templateIdentifier, entityIdentifier, effectiveDepth, includeProperties);

        EntityCompositeKey rootKey = new EntityCompositeKey(rootEntity.templateIdentifier(), rootEntity.identifier());

        // One shared visited set per request — each node is fully expanded at most once,
        // preventing O(2^depth) recursion from mutual outbound/inbound re-expansion.
        Set<String> visitedNodeIds = new HashSet<>();

        return buildGraphNode(rootKey, entityMap, effectiveDepth, includeProperties, visitedNodeIds);
    }

    /// Builds a graph node from a pre-loaded entity map (no database calls).
    ///
    /// [visitedNodeIds] tracks nodes that have already been fully built in this traversal.
    /// When a node is encountered again (cycle or shared reference), a stub leaf is returned
    /// immediately to cut the recursion — preventing the exponential blowup that arises from
    /// inbound scanning re-expanding the same nodes at every depth level.
    private EntityGraphNode buildGraphNode(EntityCompositeKey key,
                                           Map<EntityCompositeKey, Entity> entityMap,
                                           int remainingDepth,
                                           boolean includeProperties,
                                           Set<String> visitedNodeIds) {
        Entity entity = entityMap.get(key);
        if (entity == null) {
            return new EntityGraphNode(key.templateIdentifier(), key.identifier(), key.identifier(),
                    List.of(), List.of(), List.of());
        }

        // Guard: return a stub leaf if this node was already fully built in another branch.
        // This breaks both directed cycles (A→B→A) and shared references (A→B, C→B).
        // Properties are still included so data is not silently dropped for shared nodes.
        var nodeId = entity.templateIdentifier() + ":" + entity.identifier();
        if (!visitedNodeIds.add(nodeId)) {
            List<Property> stubProperties = includeProperties ? entity.properties() : List.of();
            return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
                    stubProperties, List.of(), List.of());
        }

        // Depth exhausted — return a leaf with no relations but still carry properties
        // so the deepest reachable entities expose their data when include_data=true.
        if (remainingDepth <= 0) {
            List<Property> leafProperties = includeProperties ? entity.properties() : List.of();
            return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
                    leafProperties, List.of(), List.of());
        }

        List<EntityGraphRelation> outboundRelations = entity.relations().stream()
                .map(relation -> new EntityGraphRelation(
                        relation.name(),
                        relation.targetEntityIdentifiers().stream()
                                .map(targetId -> buildGraphNode(
                                        findKeyByIdentifier(targetId, entityMap),
                                        entityMap, remainingDepth - 1, includeProperties, visitedNodeIds))
                                .toList()
                ))
                .toList();

        List<EntityGraphRelation> inboundRelations = buildRelationsAsTargetFromMap(
                entity.identifier(), entityMap, remainingDepth - 1, includeProperties, visitedNodeIds);

        List<Property> properties = includeProperties ? entity.properties() : List.of();
        return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
                properties, outboundRelations, inboundRelations);
    }

    /// Looks up a composite key from the map by identifier alone.
    /// Falls back to a synthetic key if no match is found (entity not in graph).
    private EntityCompositeKey findKeyByIdentifier(String identifier, Map<EntityCompositeKey, Entity> entityMap) {
        return entityMap.keySet().stream()
                .filter(k -> k.identifier().equals(identifier))
                .findFirst()
                .orElse(new EntityCompositeKey("", identifier));
    }

    /// Builds incoming relations (where this entity is the target) from the pre-loaded entity map.
    /// Passes [visitedNodeIds] through so that source nodes already expanded elsewhere are not
    /// re-expanded here, preventing the mutual recursion that causes OOM at high depths.
    private List<EntityGraphRelation> buildRelationsAsTargetFromMap(String targetIdentifier,
                                                                     Map<EntityCompositeKey, Entity> entityMap,
                                                                     int remainingDepth,
                                                                     boolean includeProperties,
                                                                     Set<String> visitedNodeIds) {
        Map<String, List<EntityCompositeKey>> sourcesByRelationName = new HashMap<>();

        for (Map.Entry<EntityCompositeKey, Entity> entry : entityMap.entrySet()) {
            Entity sourceEntity = entry.getValue();
            for (Relation relation : sourceEntity.relations()) {
                if (relation.targetEntityIdentifiers().contains(targetIdentifier)) {
                    sourcesByRelationName
                            .computeIfAbsent(relation.name(), k -> new ArrayList<>())
                            .add(entry.getKey());
                }
            }
        }

        return sourcesByRelationName.entrySet().stream()
                .map(e -> new EntityGraphRelation(
                        e.getKey(),
                        e.getValue().stream()
                                .map(sourceKey -> buildGraphNode(sourceKey, entityMap, remainingDepth,
                                        includeProperties, visitedNodeIds))
                                .toList()
                ))
                .toList();
    }
}
