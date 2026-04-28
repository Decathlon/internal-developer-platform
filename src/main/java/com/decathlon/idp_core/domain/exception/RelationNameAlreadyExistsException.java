package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NAME_ALREADY_EXISTS;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;

/// Exception thrown when attempting to create or update an [EntityTemplate] with duplicate relation names.
///
/// This exception represents a business rule violation where unique constraints on relation
/// names within a template are enforced at the application level.
///
/// **Why this exception exists:**
/// - Enforces business invariant that relation names must be unique within a template
/// - Provides domain-specific error information for clear API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class RelationNameAlreadyExistsException extends RuntimeException {

    /// Constructs a new exception with the duplicate relation name.
    ///
    /// @param relationName the relation name that appears more than once
    public RelationNameAlreadyExistsException(String relationName) {
        super(String.format(RELATION_NAME_ALREADY_EXISTS, relationName));
    }
}
