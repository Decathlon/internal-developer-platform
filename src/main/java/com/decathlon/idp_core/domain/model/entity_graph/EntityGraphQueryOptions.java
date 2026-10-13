package com.decathlon.idp_core.domain.model.entity_graph;

import java.util.Objects;
import java.util.Set;

/// Options controlling graph traversal and the data included in graph nodes.
///
/// @param depth maximum relationship depth requested by the caller
/// @param includeProperties whether to fetch properties for every discovered entity
/// @param includeRootEntityProperties whether to fetch properties for root entities when
///        `includeProperties` is false
/// @param relationFilter relation names to include; empty means all
/// @param propertyFilter property names to include; empty means all
/// @param mode traversal direction and flow semantics
public record EntityGraphQueryOptions(int depth, boolean includeProperties,
    boolean includeRootEntityProperties, Set<String> relationFilter, Set<String> propertyFilter,
    EntityGraphTraversalMode mode) {

  public EntityGraphQueryOptions {
    relationFilter = relationFilter == null ? Set.of() : Set.copyOf(relationFilter);
    propertyFilter = propertyFilter == null ? Set.of() : Set.copyOf(propertyFilter);
    Objects.requireNonNull(mode, "mode");
  }
}
