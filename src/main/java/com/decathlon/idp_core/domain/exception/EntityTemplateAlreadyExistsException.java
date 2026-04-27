package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_ALREADY_EXISTS;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.service.EntityTemplateService;

/// Exception thrown when attempting to create an [EntityTemplate] with an identifier that already exists.
///
/// This exception is part of the domain layer and represents a business rule violation
/// where unique constraints are enforced at the application level. It is mapped to
///
/// **Why this exception exists:**
/// - Enforces business invariant that entity template identifiers must be unique
/// - Provides domain-specific error information for clear API feedback
/// - Maintains separation of concerns between domain rules and HTTP status codes
/// - Follows DDD principles by being thrown from service layer when business rules are violated
///
/// **Usage patterns:**
/// - Thrown from [EntityTemplateService] when duplicate identifiers detected
/// - Caught by [ApiExceptionHandler] and mapped to HTTP 409 status
/// - Contains specific identifier that caused the conflict for debugging
public class EntityTemplateAlreadyExistsException extends RuntimeException {

    /// Constructs a new exception with the specific identifier that already exists.
    ///
    /// **Why this constructor exists:**
    /// - Formats exception message to include the duplicate identifier for clear debugging
    /// - Provides consistent error messaging across the application
    /// - Enables API consumers to understand which specific identifier caused the conflict
    ///
    /// @param identifier the identifier that already exists in the system, must not be null
    /// @throws IllegalArgumentException if identifier is null
    public EntityTemplateAlreadyExistsException(String identifier) {
        super(String.format(TEMPLATE_ALREADY_EXISTS + ":%s", identifier));
    }
}
