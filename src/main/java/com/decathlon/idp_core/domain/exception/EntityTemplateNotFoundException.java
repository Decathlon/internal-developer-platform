package com.decathlon.idp_core.domain.exception;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

/// Domain exception for missing [EntityTemplate] business entities.
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to access an EntityTemplate that doesn't exist in the system. This is a
/// critical business error since entities cannot be created without valid templates.
///
/// **Exception design rationale:**
/// - Multiple constructors support different lookup scenarios (ID, identifier, field-based)
/// - Meaningful error messages aid in debugging and API error responses
/// - Domain-level exception keeps business logic separate from HTTP concerns
///
/// **Usage patterns:**
/// - Template validation before entity operations
/// - Template-based entity queries
/// - Template management operations
public class EntityTemplateNotFoundException extends RuntimeException {

    /// Default constructor for generic template not found scenarios.
    ///
    /// **Why this exists:** Provides a fallback when specific template details
    /// are not available but the business rule violation still needs to be reported.
    public EntityTemplateNotFoundException() {
        super("Template not found");
    }

    /// Constructs a new exception with a custom error message.
    ///
    /// **Why this exists:** Allows for specific error messages that provide more
    /// context about the search criteria or operation that failed.
    ///
    /// @param message the detail message explaining what was not found
    public EntityTemplateNotFoundException(String message) {
        super(message);
    }

    /// Constructs a new exception for a specific UUID-based lookup.
    ///
    /// **Why this exists:** Provides standardized error message format when
    /// searching for a template by its primary key identifier.
    ///
    /// @param id the UUID of the template that was not found
    public EntityTemplateNotFoundException(UUID id) {
        super("Template not found with ID: " + id);
    }

    /// Constructs a new exception for field-based searches.
    ///
    /// **Why this exists:** Commonly used for business identifier searches where
    /// the field name (e.g., "identifier") and its value are known, providing
    /// clear context about what search criteria failed.
    ///
    /// @param fieldName the name of the field used in the search (e.g., "identifier")
    /// @param value the value that was searched for but not found
    public EntityTemplateNotFoundException(String fieldName, String value) {
        super("Template not found with " + fieldName + ": " + value);
    }

    /// Constructs a new exception with a custom message and underlying cause.
    ///
    /// **Why this exists:** Used when the exception wraps another exception or
    /// when additional context about the underlying cause is needed for debugging.
    ///
    /// @param message the detail message explaining what was not found
    /// @param cause the underlying cause of this exception
    public EntityTemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
