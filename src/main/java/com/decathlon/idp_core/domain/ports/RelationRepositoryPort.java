package com.decathlon.idp_core.domain.ports;

import java.util.List;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;

/// Driven port defining the contract for [Relation] specialized query operations.
///
/// **Contract expectations for implementations:**
/// - `findRelationsSummariesByTargetEntityIdentifiers()` must return all incoming relations
///   for the specified target entities, enabling reverse relationship navigation
/// - Results must be accurate and consistent with the current state of entity relationships
/// - Performance should be optimized for bulk lookups when multiple target identifiers provided
///
/// **Business purpose:** Supports dependency analysis, relationship impact assessment,
/// and bidirectional navigation through the entity relationship graph.
public interface RelationRepositoryPort {

    List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(
            List<String> targetEntityIdentifiers);
}
