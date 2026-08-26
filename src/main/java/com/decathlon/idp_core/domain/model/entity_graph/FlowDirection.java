package com.decathlon.idp_core.domain.model.entity_graph;

/// Internal flow direction states used during graph traversal.
///
/// Maps to relationship directions (outbound vs inbound) during BFS propagation.
/// This enum is used internally by [EntityGraphHelper] to track traversal flow
/// and should not be exposed in public APIs.
public enum FlowDirection {
  /// Follow outbound relationships only (downstream dependencies)
  OUTBOUND,
  /// Follow inbound relationships only (upstream dependents)
  INBOUND,
  /// Follow both outbound and inbound relationships
  ANY
}
