package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/// Unified Entity Data Transfer Object for API responses.
///
/// Combines outbound relations (where this entity is the source) and inbound
/// relations (where this entity is the target) into a single relations map,
/// keyed by relation name.
///
/// **Immutability:** All mutable fields are wrapped in unmodifiable collections
/// to prevent external modification through the DTO accessor methods. The compact
/// constructor ensures incoming maps are immediately wrapped before storage.
@JsonNaming(SnakeCaseStrategy.class)
public record EntityDtoOut(String identifier, String name, String templateIdentifier,
    Map<String, Object> properties, Map<String, List<EntitySummaryDto>> relations) {

  /// Compact constructor that wraps mutable maps in unmodifiable copies.
  ///
  /// **Purpose:** Prevents external code from modifying the internal state
  /// through the accessor methods. Each call to `properties()` or `relations()`
  /// returns an unmodifiable view of the original map.
  ///
  /// **Performance note:** Unmodifiable wrappers have minimal overhead (single
  /// wrapper object) and are preferred over defensive copying.
  public EntityDtoOut {
    properties = Collections.unmodifiableMap(properties != null ? properties : Map.of());
    relations = Collections.unmodifiableMap(relations != null ? relations : Map.of());
  }
}
