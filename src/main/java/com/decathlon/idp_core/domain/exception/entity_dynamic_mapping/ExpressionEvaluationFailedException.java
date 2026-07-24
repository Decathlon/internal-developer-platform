package com.decathlon.idp_core.domain.exception.entity_dynamic_mapping;

import lombok.Getter;

/**
 * Thrown when JSLT expression evaluation fails on both current node and
 * fallback root payload. Indicates a critical mapping configuration issue: the
 * expression is either syntactically invalid, references non-existent paths, or
 * the payload structure does not match the expected mapping definition.
 */
@Getter
public class ExpressionEvaluationFailedException extends RuntimeException {

  private final String expression;
  private final String reason;

  /**
   * Creates a new exception with expression and failure reason.
   *
   * @param expression
   *          the JSLT expression that failed to evaluate
   * @param reason
   *          human-readable description of why evaluation failed
   * @param cause
   *          the original exception that triggered this failure
   */
  public ExpressionEvaluationFailedException(String expression, String reason, Throwable cause) {
    super(String.format("Expression evaluation failed for '%s': %s", expression, reason), cause);
    this.expression = expression;
    this.reason = reason;
  }

  /**
   * Creates a new exception with expression and failure reason (no cause chain).
   *
   * @param expression
   *          the JSLT expression that failed to evaluate
   * @param reason
   *          human-readable description of why evaluation failed
   */
  public ExpressionEvaluationFailedException(String expression, String reason) {
    super(String.format("Expression evaluation failed for '%s': %s", expression, reason));
    this.expression = expression;
    this.reason = reason;
  }
}
