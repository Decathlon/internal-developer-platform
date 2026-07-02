package com.decathlon.idp_core.domain.port;

import java.util.Map;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;

/// Driven port defining the contract for entity relationship graph retrieval.
///
/// Separated from [EntityRepositoryPort] to follow the Interface Segregation
/// Principle: graph traversal is a distinct read concern backed by recursive CTE
/// queries, with no overlap with standard CRUD operations.
///
/// **Contract expectations for implementations:**
/// - Must traverse both outbound and inbound relations up to the requested depth
/// - Must return the root entity itself as part of the result map
/// - Must return an empty map when the root entity does not exist
/// - Depth must be clamped server-side; implementations may ignore values
///   outside a valid range
///
/// **Transaction behavior:** Implementations should use a read-only transaction
/// as this port performs no write operations.
public interface EntityGraphRepositoryPort {

  /// Fetches all entities in the relationship graph rooted at the given entity
  /// UUID.
  ///
  /// Performs an optimized recursive CTE (Common Table Expression) database query
  /// to traverse both outbound (forward) and inbound (reverse) relations up to
  /// the
  /// requested depth. All loaded entities are batch-fetched to minimize database
  /// round trips.
  ///
  /// **Key design notes:**
  /// - Relation name filtering is intentionally NOT pushed into this port. The
  /// CTE
  /// always traverses all relation types so that nodes reachable via any path
  /// are loaded. Filtering (e.g., "only 'owns' relations") is applied in the
  /// service layer, allowing for edge filtering without re-querying. Example:
  /// "filter owns" still returns B and C when A→(depends-on)→B→(owns)→C.
  /// - The traversal mode determines which relation directions are included.
  ///
  /// @param entityId the root entity UUID from which the graph is
  /// traversed
  /// @param depth the maximum traversal depth; typically clamped to
  /// 1-6 by the service layer
  /// @param includeProperties when true, entity properties are eagerly loaded
  /// along with the root entity and all reachable
  /// entities
  /// @param mode the graph traversal mode (BIDIRECTIONAL,
  /// OUTBOUND_ONLY, or STRICT_LINEAGE) determining which
  /// relation directions to follow
  /// @return an immutable map of all discovered entities keyed by their UUID,
  /// including the root entity. Returns an empty map if the root entity
  /// does not exist.
  Map<UUID, Entity> findEntityGraph(UUID entityId, int depth, boolean includeProperties,
      EntityGraphTraversalMode mode);

  /// Fetches relationship graphs for multiple root entities in a batch operation.
  ///
  /// Equivalent to calling [findEntityGraph] for each root entity, but optimized
  /// to use a single batch recursive CTE query for better database performance.
  ///
  /// @param rootIds list of root entity UUIDs from which graphs are
  /// traversed
  /// @param depth the maximum traversal depth; typically clamped to
  /// 1-6 by the service layer
  /// @param includeProperties when true, entity properties are eagerly loaded
  /// @param mode the graph traversal mode determining which relation
  /// directions to follow
  /// @return an immutable map of all discovered entities keyed by their UUID,
  /// including all root entities and their reachable neighbors
  Map<UUID, Entity> findEntityGraphBatch(java.util.List<UUID> rootIds, int depth,
      boolean includeProperties, EntityGraphTraversalMode mode);

  Map<UUID, Entity> findEntityGraphBatchByTemplate(java.util.List<UUID> rootIds, int depth,
      String startTemplate, int size, int offset);

  public Map<UUID, Entity> findEntityGraphByAgnosticTemplate(UUID[] rootIds, String[] groupIds,
      long expectedGroupCount, int depth, String startTemplate, int size, int offset);
}
