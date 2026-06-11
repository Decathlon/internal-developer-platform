package com.decathlon.idp_core.domain.model.entity_graph;

/// Defines the traversal mode for entity graph queries.
///
/// - **STRICT_LINEAGE**: Follow only outbound relations (forward dependencies)
/// - **BIDIRECTIONAL**: Follow both outbound and inbound relations (full graph)
/// - **OUTBOUND_ONLY**: Follow only outbound relations without inbound lookups
public enum EntityGraphTraversalMode {
  STRICT_LINEAGE, BIDIRECTIONAL, OUTBOUND_ONLY;

  // @Override
  // public String toString() {
  // return name();
  // }
}