package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_ALREADY_EXISTS;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.service.EntityTemplateService;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;

/// Exception thrown when attempting to create or update an [EntityTemplate] with a name that already exists.
///
/// This exception is part of the domain layer and represents a business rule violation
/// where unique constraints on template names are enforced at the application level.
/// It is mapped to HTTP 409 (Conflict) status by [ApiExceptionHandler].
///
/// **Why this exception exists:**
/// - Enforces business invariant that entity template names must be unique
/// - Provides domain-specific error information for clear API feedback
/// - Maintains separation of concerns between domain rules and HTTP status codes
/// - Follows DDD principles by being thrown from service layer when business rules are violated
///
/// **Usage patterns:**
/// - Thrown from [EntityTemplateService] when duplicate names detected
/// - Caught by [ApiExceptionHandler] and mapped to HTTP 409 status
/// - Contains specific name that caused the conflict for debugging
public class EntityTemplateNameAlreadyExistsException extends RuntimeException {

    /// Constructs a new exception with the specific template name that already exists.
    ///
    /// **Why this constructor exists:**
    /// - Formats exception message to include the duplicate name for clear debugging
    /// - Provides consistent error messaging across the application
    /// - Enables API consumers to understand which specific name caused the conflict
    ///
    /// @param name the template name that already exists in the system, must not be null
    /// @throws IllegalArgumentException if name is null
    public EntityTemplateNameAlreadyExistsException(String name) {
        super(String.format(TEMPLATE_NAME_ALREADY_EXISTS, name));
    }
}
