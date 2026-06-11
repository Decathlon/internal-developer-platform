package com.decathlon.idp_core.domain.port;

import java.util.Map;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;

/// Driven port defining the contract for entity relationship graph retrieval.
///
/// Separated from [EntityRepositoryPort] to follow the Interface Segregation Principle:
/// graph traversal is a distinct read concern backed by recursive CTE queries,
/// with no overlap with standard CRUD operations.
///
/// **Contract expectations for implementations:**
/// - Must traverse both outbound and inbound relations up to the requested depth
/// - Must return the root entity itself as part of the result map
/// - Must return an empty map when the root entity does not exist
/// - Depth must be clamped server-side; implementations may ignore values outside a valid range
///
/// **Transaction behavior:** Implementations should use a read-only transaction
/// as this port performs no write operations.
public interface EntityGraphRepositoryPort {

  /// Fetches all entities in the relationship graph rooted at the given composite
  /// key.
  ///
  /// Uses a recursive CTE to traverse both outbound and inbound relations up to
  /// the
  /// specified depth, then batch-loads all entities in a minimal number of
  /// queries.
  ///
  /// @param templateIdentifier the template identifier of the root entity
  /// @param entityIdentifier the business identifier of the root entity within
  /// its template
  /// @param depth the maximum traversal depth (1-10)
  /// @param includeProperties when true, entity properties are loaded along with
  /// root not found
  /// Relation name filtering is intentionally NOT pushed into this port.
  /// The CTE always traverses all relation types so that nodes reachable via
  /// any path are loaded. Edge filtering is applied in the service layer so
  /// that "filter owns" still returns B and C when A→(depends-on)→B→(owns)→C.
  Map<UUID, Entity> findEntityGraph(UUID entityId, int depth, boolean includeProperties,
      EntityGraphTraversalMode mode);

}
