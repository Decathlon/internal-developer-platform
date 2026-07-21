package com.decathlon.idp_core.domain.port;

import java.util.List;
import java.util.Map;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;

/// Port for dynamic entity mapping operations.
///
/// This port abstracts the underlying expression engine (JSLT, JQ, etc.)
/// allowing the domain layer to remain engine-agnostic.
public interface MappingEnginePort {

  /**
   * Evaluates mapping against raw payload. Supports both single entity mapping
   * and multi-entity list generation.
   *
   * @return List of mapped entities, or empty list if filtered out / skipped.
   */
  List<Entity> mapToEntities(String rawPayload, EntityDynamicMapping mapping);

  /// Extracts values from a payload using the mapping's property expressions.
  ///
  /// @param rawPayload the raw JSON string
  /// @param propertyExpressions map of property name to expression
  /// @return map of property name to extracted value
  Map<String, Object> extractProperties(String rawPayload, Map<String, String> propertyExpressions);
}
