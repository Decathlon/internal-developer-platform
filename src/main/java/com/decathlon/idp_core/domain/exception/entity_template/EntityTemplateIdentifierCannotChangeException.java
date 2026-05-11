package com.decathlon.idp_core.domain.exception.entity_template;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IDENTIFIER_CANNOT_CHANGE;

/// Exception thrown when attempting to change an [EntityTemplate] identifier after creation.
///
/// **Why this exception exists:**
/// - Entity template identifiers are immutable once the template is created
/// - Prevents accidental or malicious modifications to template identity
/// - Maintains separation of concerns between domain rules and HTTP status codes
///
/// **Usage patterns:**
/// - Thrown from [EntityTemplateService] when identifier modification is attempted
/// - Caught by [ApiExceptionHandler] and mapped to HTTP 400 status
/// - Contains the identifier that was attempted to be changed for debugging
public class EntityTemplateIdentifierCannotChangeException extends RuntimeException {

    public EntityTemplateIdentifierCannotChangeException(String identifier) {
        super(TEMPLATE_IDENTIFIER_CANNOT_CHANGE + identifier);
    }
}
