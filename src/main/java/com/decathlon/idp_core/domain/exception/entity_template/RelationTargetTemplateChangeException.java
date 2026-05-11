package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_TEMPLATE_CANNOT_CHANGE;

/// Exception thrown when attempting to change the target template of an existing relation definition.
///
/// This exception represents a business rule violation where target template changes are blocked
/// to prevent relational inconsistencies with existing entity data.
///
/// **Why this exception exists:**
/// - Protects data integrity by preventing target template changes that would invalidate
///   existing entity relation values pointing to the old template type
/// - Provides domain-specific error information for clear API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class RelationTargetTemplateChangeException extends RuntimeException {

    /// Constructs a new exception for a target template change attempt.
    ///
    /// @param relationName  the name of the relation whose target is being changed
    /// @param fromTarget    the current target template identifier
    /// @param toTarget      the requested new target template identifier
    public RelationTargetTemplateChangeException(String relationName, String fromTarget, String toTarget) {
        super(String.format(RELATION_TARGET_TEMPLATE_CANNOT_CHANGE, relationName, fromTarget, toTarget));
    }
}
