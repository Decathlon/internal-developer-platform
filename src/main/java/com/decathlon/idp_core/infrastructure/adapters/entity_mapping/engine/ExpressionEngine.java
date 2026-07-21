package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.engine;

import com.fasterxml.jackson.databind.JsonNode;

/// Interface for expression evaluation engines (JSLT, JQ, etc.).
///
/// This abstraction allows swapping the underlying DSL engine without
/// affecting higher-level mapping components. See ADR-0004.
public interface ExpressionEngine {

  /// Validates that the given expression is syntactically correct.
  ///
  /// @param expression the DSL expression to validate
  /// @throws
  /// com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException
  /// if the expression is invalid
  void validateExpression(String expression);

  /// Evaluates an expression against a JSON payload.
  ///
  /// @param expression the DSL expression
  /// @param payload the input JSON
  /// @return the evaluation result as a JsonNode
  JsonNode evaluate(String expression, JsonNode payload);
}
