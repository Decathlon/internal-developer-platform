package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_ALREADY_EXISTS;

/**
 * Exception thrown when attempting to create an Entity Template with an identifier that already exists.
 * <p>
 * This exception is part of the domain layer and represents a business rule violation
 * where unique constraints are enforced at the application level. It is mapped to
 * HTTP 409 (Conflict) status by the {@code ApiExceptionHandler}.
 * </p>
 * <p>
 * The exception follows Domain-Driven Design principles by:
 * <ul>
 *   <li>Being thrown from the service layer when business rules are violated</li>
 *   <li>Containing domain-specific error information</li>
 *   <li>Providing clear messaging for API consumers</li>
 * </ul>
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see com.decathlon.idp_core.infrastructure.api.handler.ApiExceptionHandler
 */
public class EntityTemplateAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new EntityTemplateAlreadyExistsException with a specific identifier.
     * <p>
     * The exception message is formatted to include the duplicate identifier that
     * caused the conflict, providing clear feedback for debugging and API responses.
     * </p>
     *
     * @param identifier the identifier that already exists in the system, must not be null
     * @throws IllegalArgumentException if identifier is null
     */
    public EntityTemplateAlreadyExistsException(String identifier) {
        super(String.format(TEMPLATE_ALREADY_EXISTS + ":%s", identifier));
    }
}
