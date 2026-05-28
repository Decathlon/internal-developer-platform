package com.decathlon.idp_core.domain.model.entity_graph;

import java.util.List;

import com.decathlon.idp_core.domain.model.entity.Property;

/// A node in the entity relationship graph, containing summary information
/// and its resolved relations (recursively up to a configurable depth).
///
/// **Business purpose:**
/// - Visualizing entity dependency graphs
/// - Understanding relationship chains between entities
/// - Providing a hierarchical view of entity connections
///
/// @param templateIdentifier the template identifier this entity belongs to
/// @param identifier the business identifier of the entity
/// @param name the human-readable name of the entity
/// @param properties the entity's property instances; empty when not requested
/// @param relations the resolved outbound relations with their target graph nodes
/// @param relationsAsTarget incoming relations where this entity is the target
public record EntityGraphNode(String templateIdentifier, String identifier, String name,
    List<Property> properties, List<EntityGraphRelation> relations,
    List<EntityGraphRelation> relationsAsTarget) {
  public EntityGraphNode {
    properties = properties != null ? List.copyOf(properties) : List.of();
    relations = relations != null ? List.copyOf(relations) : List.of();
    relationsAsTarget = relationsAsTarget != null ? List.copyOf(relationsAsTarget) : List.of();
  }
}
