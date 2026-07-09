package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IS_RELATION_TARGET;

/// Exception thrown when attempting to delete a template that is still referenced
/// as a `targetTemplateIdentifier` in another template's relation definition.
///
/// This exception enforces the template relation target integrity business rule:
/// a template may not be deleted while other templates declare relations that
/// point to it as the target, because doing so would create dangling relation
/// definitions and break the integrity of those templates.
///
/// **Why this exception exists:**
/// - Prevents orphaned relation definitions in other templates
/// - Provides a clear, domain-specific error for API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class EntityTemplateIsRelationTargetException extends RuntimeException {

  /// Constructs a new exception for the template identifier that cannot be
  /// deleted.
  ///
  /// @param identifier the identifier of the template blocked from deletion
  public EntityTemplateIsRelationTargetException(String identifier) {
    super(String.format(TEMPLATE_IS_RELATION_TARGET, identifier));
  }
}
