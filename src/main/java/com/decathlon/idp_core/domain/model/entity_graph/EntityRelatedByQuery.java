package com.decathlon.idp_core.domain.model.entity_graph;

import java.util.Objects;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.model.search.SortCriteria;

/// Immutable query specification for retrieving paginated entities reached via a
/// terminal relation name.
///
/// @param rootId UUID of the starting root entity
/// @param relationName name of the terminal relation edge leading to matched
///        entities
/// @param depth maximum number of relationship hops (`1..6`)
/// @param mode directional flow rules applied during graph traversal
/// @param paginationCriteria validated zero-based page and page size
/// @param sortCriteria validated sort field and direction
public record EntityRelatedByQuery(UUID rootId, String relationName, int depth,
    EntityGraphTraversalMode mode, PaginationCriteria paginationCriteria,
    SortCriteria sortCriteria) {

  /// Validates non-null query invariants on instantiation.
  public EntityRelatedByQuery {
    Objects.requireNonNull(rootId, "rootId");
    Objects.requireNonNull(relationName, "relationName");
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(paginationCriteria, "paginationCriteria");
    Objects.requireNonNull(sortCriteria, "sortCriteria");
  }
}
