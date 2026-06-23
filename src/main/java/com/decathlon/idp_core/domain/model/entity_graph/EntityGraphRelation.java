package com.decathlon.idp_core.domain.model.entity_graph;

import java.util.List;

/// Represents a single named relation in the entity graph with its resolved target nodes.
///
/// **Business purpose:**
/// - Groups related entities under their relation name
/// - Enables graph traversal by relation type
///
/// @param name the relation name as defined in the entity template
/// @param targets the resolved target entity graph nodes (recursively populated up to depth)
public record EntityGraphRelation(String name, List<EntityGraphNode> targets) {
  public EntityGraphRelation {
    targets = targets != null ? List.copyOf(targets) : List.of();
  }
}
