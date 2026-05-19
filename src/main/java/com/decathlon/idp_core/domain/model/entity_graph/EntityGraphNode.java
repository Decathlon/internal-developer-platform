package com.decathlon.idp_core.domain.model.entity_graph;

import java.util.List;

/// A node in the entity relationship graph, containing summary information
/// and its resolved relations (recursively up to a configurable depth).
///
/// **Business purpose:**
/// - Visualizing entity dependency graphs
/// - Understanding relationship chains between entities
/// - Providing a hierarchical view of entity connections
///
/// @param summary the lightweight entity identification data
/// @param relations the resolved outbound relations with their target graph nodes
/// @param relationsAsTarget incoming relations where this entity is the target
public record EntityGraphNode(
        String identifier,
        String name,
        List<EntityGraphRelation> relations,
        List<EntityGraphRelation> relationsAsTarget
) {
    public EntityGraphNode {
        relations = relations != null ? List.copyOf(relations) : List.of();
        relationsAsTarget = relationsAsTarget != null ? List.copyOf(relationsAsTarget) : List.of();
    }
}
