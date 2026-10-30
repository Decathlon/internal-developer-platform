package com.decathlon.idp_core.domain.model.enums;

/// Controls how relation target identifiers are checked during entity writes.
public enum RelationTargetValidationMode {
  /// Requires every non-blank relation target identifier to resolve to an entity.
  STRICT,

  /// Filters unresolved identifiers from optional relations.
  TOLERANT
}
