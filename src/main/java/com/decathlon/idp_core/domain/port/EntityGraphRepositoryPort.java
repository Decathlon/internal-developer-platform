package com.decathlon.idp_core.domain.port;

import java.util.Collection;
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

  /// Fetches entities reachable from one or more root entity UUIDs.
  ///
  /// Performs a recursive CTE traversal up to the requested depth, then loads
  /// entity summaries, relations, and the requested properties in a batch.
  ///
  /// **Key design notes:**
  /// - Relation name filtering is intentionally NOT pushed into this port. The
  /// CTE always traverses all relation types so that nodes reachable via any
  /// path are loaded. Filtering (e.g., "only 'owns' relations") is applied in
  /// the service layer, allowing for edge filtering without re-querying.
  /// Example: "filter owns" still returns B and C when A→(depends-on)→B→(owns)→C.
  /// - The traversal mode determines which relation directions are included.
  ///
  /// @param rootIds UUIDs of entities from which traversal starts
  /// @param depth maximum traversal depth; callers of the single-root service
  /// clamp this to the supported range before calling the port
  /// @param includeProperties when true, include properties for every discovered
  /// entity
  /// @param showRootEntityProperties when true and `includeProperties` is
  /// false, include properties only for entities in `rootIds`
  /// @param mode traversal mode determining which relation directions to follow
  /// @return map of discovered entities keyed by UUID; empty when no entities are
  /// discovered or the inputs cannot start a traversal
  Map<UUID, Entity> findEntityGraph(Collection<UUID> rootIds, int depth, boolean includeProperties,
      boolean showRootEntityProperties, EntityGraphTraversalMode mode);

}
