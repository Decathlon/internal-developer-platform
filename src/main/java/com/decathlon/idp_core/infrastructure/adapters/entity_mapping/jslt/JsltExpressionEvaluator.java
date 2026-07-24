package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.ExpressionEvaluationFailedException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Utility component for JSLT expression resolution and payload traversal.
///
/// Responsibilities:
/// - Resolve expressions with fallback from current node to root payload.
/// - Stream payload items from array or object-shaped inputs.
/// - Locate the first non-empty array field in an object payload.
@Slf4j
@Component
@RequiredArgsConstructor
public class JsltExpressionEvaluator {

  private final JsltEngine jsltEngine;

  /// Resolves an expression on current node, then falls back to root payload.
  ///
  /// Resolution order:
  /// - Evaluate on currentNode.
  /// - Retry on rootPayload when value is missing or evaluation fails.
  /// - Throw ExpressionEvaluationFailedException when both attempts fail.
  public JsonNode resolveExpression(String expression, JsonNode currentNode, JsonNode rootPayload) {
    try {
      JsonNode result = jsltEngine.evaluate(expression, currentNode);
      if (result != null && !result.isNull() && !result.isMissingNode()) {
        return result;
      }
      log.debug("Expression evaluation on current node returned null/missing. "
          + "Retrying with root payload. Expression: '{}'", expression);
    } catch (Exception currentNodeException) {
      log.debug(
          "Expression evaluation on current node failed: {}. "
              + "Retrying with root payload. Expression: '{}'",
          currentNodeException.getMessage(), expression);

      return resolveOnRootOrFail(expression, rootPayload, String.format(
          "Failed on current node (%s) and root payload (%%s)", currentNodeException.getMessage()));
    }

    return resolveOnRootOrFail(expression, rootPayload,
        "Current node returned null/missing, root payload evaluation failed: %s");
  }

  /// Resolves an expression against root payload or throws a domain exception.
  private JsonNode resolveOnRootOrFail(String expression, JsonNode rootPayload,
      String reasonTemplate) {
    try {
      return jsltEngine.evaluate(expression, rootPayload);
    } catch (Exception rootException) {
      throw new ExpressionEvaluationFailedException(expression,
          String.format(reasonTemplate, rootException.getMessage()), rootException);
    }
  }

  /// Finds the first non-empty array field in an object payload.
  public Optional<JsonNode> findFirstArray(JsonNode rootObject) {
    return StreamSupport
        .stream(java.util.Spliterators.spliteratorUnknownSize(rootObject.properties().iterator(),
            java.util.Spliterator.ORDERED), false)
        .map(java.util.Map.Entry::getValue).filter(JsonNode::isArray)
        .filter(node -> !node.isEmpty()).findFirst();
  }
}
