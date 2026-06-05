package com.decathlon.idp_core.domain.model.search;

/// Logical connectors for combining multiple filter nodes in a search query.
///
/// **Business semantics:**
/// - [AND] — all child nodes must match
/// - [OR] — at least one child node must match
public enum LogicalConnector {
  AND, OR
}
