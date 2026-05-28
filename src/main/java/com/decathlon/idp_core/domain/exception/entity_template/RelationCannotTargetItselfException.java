package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_CANNOT_TARGET_ITSELF;

/// Exception thrown when a relation definition's target template identifier refers to
/// the template that owns the relation.
///
/// **Why this exception exists:**
/// - Prevents circular self-referential relations at the template level
/// - A template relating to itself creates logical ambiguity and is blocked by design
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class RelationCannotTargetItselfException extends RuntimeException {

  /// Constructs a new exception for a self-referential relation attempt.
  ///
  /// @param relationName the name of the relation pointing to its own template
  /// @param templateIdentifier the identifier of the template that is both owner
  /// and target
  public RelationCannotTargetItselfException(String relationName, String templateIdentifier) {
    super(String.format(RELATION_CANNOT_TARGET_ITSELF, relationName, templateIdentifier));
  }
}
