package com.decathlon.idp_core.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when an Entity Template cannot be found in the system.
 * <p>
 * This exception is part of the domain layer and represents a case where
 * a requested Entity Template does not exist. It is mapped to HTTP 404 (Not Found)
 * status by the {@code ApiExceptionHandler}.
 * </p>
 * <p>
 * The exception provides multiple constructors to support different scenarios:
 * <ul>
 *   <li>Generic template not found</li>
 *   <li>Template not found by UUID</li>
 *   <li>Template not found by field name and value (e.g., identifier)</li>
 *   <li>Custom message with optional cause</li>
 * </ul>
 * </p>
 * <p>
 * This follows Domain-Driven Design principles by encapsulating domain-specific
 * error conditions and providing meaningful error messages for API consumers.
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see com.decathlon.idp_core.infrastructure.api.handler.ApiExceptionHandler
 */
public class EntityTemplateNotFoundException extends RuntimeException {

    /**
     * Constructs a new EntityTemplateNotFoundException with a default message.
     * <p>
     * This constructor is used for generic cases where no specific details
     * are available about what was not found.
     * </p>
     */
    public EntityTemplateNotFoundException() {
        super("Template not found");
    }

    /**
     * Constructs a new EntityTemplateNotFoundException with a custom message.
     * <p>
     * This constructor allows for specific error messages that provide more
     * context about the search criteria or operation that failed.
     * </p>
     *
     * @param message the detail message explaining what was not found
     */
    public EntityTemplateNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new EntityTemplateNotFoundException for a specific UUID.
     * <p>
     * This constructor is used when searching for a template by its primary key
     * and provides a standardized error message format.
     * </p>
     *
     * @param id the UUID of the template that was not found
     */
    public EntityTemplateNotFoundException(UUID id) {
        super("Template not found with ID: " + id);
    }

    /**
     * Constructs a new EntityTemplateNotFoundException for a specific field search.
     * <p>
     * This constructor is commonly used for business identifier searches where
     * the field name (e.g., "identifier") and its value are known.
     * </p>
     *
     * @param fieldName the name of the field used in the search (e.g., "identifier")
     * @param value the value that was searched for but not found
     */
    public EntityTemplateNotFoundException(String fieldName, String value) {
        super("Template not found with " + fieldName + ": " + value);
    }

    /**
     * Constructs a new EntityTemplateNotFoundException with a custom message and cause.
     * <p>
     * This constructor is used when the exception wraps another exception or
     * when additional context about the underlying cause is needed.
     * </p>
     *
     * @param message the detail message explaining what was not found
     * @param cause the underlying cause of this exception
     */
    public EntityTemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
