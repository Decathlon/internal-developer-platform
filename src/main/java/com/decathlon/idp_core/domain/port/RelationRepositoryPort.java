package com.decathlon.idp_core.domain.port;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;

/// Driven port defining the contract for Relation specialized query operations.
///
/// **Contract expectations for implementations:**
/// - `findRelationsSummariesByTargetEntityUuids()` must return all incoming relations
///   for the specified target entities using entity UUIDs (database primary keys)
/// - UUID-based matching guarantees correct entity identification regardless of
///   identifier duplicates across templates
/// - Results must be accurate and consistent with the current state of entity relationships
/// - Performance should be optimized for bulk lookups when multiple UUIDs provided
///
/// **Business purpose:** Supports dependency analysis, relationship impact assessment,
/// and bidirectional navigation through the entity relationship graph with guaranteed
/// correct entity identification.
public interface RelationRepositoryPort {

  /// Finds all incoming relations where specified entities (by UUID) are targets.
  ///
  /// @param targetEntityUuids list of entity UUIDs to find inbound relations for
  /// @return relation summaries showing all inbound connections to the specified
  /// target entities
  List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityUuids(
      List<UUID> targetEntityUuids);
}
