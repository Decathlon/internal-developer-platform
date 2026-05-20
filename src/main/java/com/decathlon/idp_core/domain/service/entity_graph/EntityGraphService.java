package com.decathlon.idp_core.domain.service.entity_graph;

import java.util.List;
import java.util.Map;

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
/// - Does not detect cycles — relies on depth limit to terminate
@Service
@RequiredArgsConstructor
public class EntityGraphService {

    private static final int MAX_DEPTH = 10;

    private final EntityRepositoryPort entityRepositoryPort;
    private final EntityGraphRepositoryPort entityGraphRepositoryPort;

    /// Builds the relationship graph for an entity starting from its composite key.
    ///
    /// **Optimization:** Uses a recursive CTE to fetch all entities in the graph in 2 queries
    /// (1 for composite key pairs, 1 for batch loading), regardless of depth.
    ///
    /// @param templateIdentifier the template identifier of the root entity
    /// @param entityIdentifier the business identifier of the root entity
    /// @param depth the maximum traversal depth (clamped to [1, MAX_DEPTH])
    /// @param includeProperties when true, each graph node carries the entity's full property list;
    ///                          when false, properties are omitted to reduce response size
    /// @return the root graph node with resolved relations
    /// @throws EntityNotFoundException when no entity matches the given identifiers
    @Transactional(readOnly = true)
    public EntityGraphNode getEntityGraph(String templateIdentifier, String entityIdentifier, int depth,
            boolean includeProperties) {
        int effectiveDepth = Math.clamp(depth, 1, MAX_DEPTH);

        // Verify root entity exists before fetching the graph
        Entity rootEntity = entityRepositoryPort
                .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

        // Optimized batch fetch: load all entities in the graph keyed by composite key
        Map<EntityCompositeKey, Entity> entityMap = entityGraphRepositoryPort
                .findEntityGraph(templateIdentifier, entityIdentifier, effectiveDepth);

        EntityCompositeKey rootKey = new EntityCompositeKey(rootEntity.templateIdentifier(), rootEntity.identifier());

        // Build the graph from pre-loaded entities (no more database calls)
        return buildGraphNode(rootKey, entityMap, effectiveDepth, includeProperties);
    }

    /// Builds a graph node from a pre-loaded entity map (no database calls).
    /// Recursively resolves both outbound and inbound relations from the cached entities.
    private EntityGraphNode buildGraphNode(EntityCompositeKey key,
                                           Map<EntityCompositeKey, Entity> entityMap,
                                           int remainingDepth,
                                           boolean includeProperties) {
        Entity entity = entityMap.get(key);
        if (entity == null) {
            return new EntityGraphNode(key.templateIdentifier(), key.identifier(), key.identifier(),
                    List.of(), List.of(), List.of());
        }

        if (remainingDepth <= 0) {
            return new EntityGraphNode(entity.templateIdentifier(), entity.identifier(), entity.name(),
                    List.of(), List.of(), List.of());
        }

        // Resolve outbound relations from pre-loaded entities
        List<EntityGraphRelation> outboundRelations = entity.relations().stream()
                .map(relation -> new EntityGraphRelation(
                        relation.name(),
                        relation.targetEntityIdentifiers().stream()
                                .map(targetId -> {
                                    // Relations only store identifier; look up by identifier across all entries
                                    EntityCompositeKey targetKey = findKeyByIdentifier(targetId, entityMap);
                                    return buildGraphNode(targetKey, entityMap, remainingDepth - 1, includeProperties);
                                })
                                .toList()
                ))
                .toList();

        // Resolve inbound relations from pre-loaded entities
        List<EntityGraphRelation> inboundRelations = buildRelationsAsTargetFromMap(
                entity.identifier(), entityMap, remainingDepth - 1, includeProperties);

        // Include properties only when explicitly requested to keep responses lean
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
    /// Scans all entities to find relations pointing to this entity.
    private List<EntityGraphRelation> buildRelationsAsTargetFromMap(String targetIdentifier,
                                                                     Map<EntityCompositeKey, Entity> entityMap,
                                                                     int remainingDepth,
                                                                     boolean includeProperties) {
        Map<String, List<EntityCompositeKey>> sourcesByRelationName = new java.util.HashMap<>();

        for (Map.Entry<EntityCompositeKey, Entity> entry : entityMap.entrySet()) {
            Entity sourceEntity = entry.getValue();
            for (Relation relation : sourceEntity.relations()) {
                if (relation.targetEntityIdentifiers().contains(targetIdentifier)) {
                    sourcesByRelationName
                            .computeIfAbsent(relation.name(), k -> new java.util.ArrayList<>())
                            .add(entry.getKey());
                }
            }
        }

        return sourcesByRelationName.entrySet().stream()
                .map(e -> new EntityGraphRelation(
                        e.getKey(),
                        e.getValue().stream()
                                .map(sourceKey -> buildGraphNode(sourceKey, entityMap, remainingDepth, includeProperties))
                                .toList()
                ))
                .toList();
    }
}
