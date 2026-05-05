package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_ALREADY_EXISTS;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.service.EntityTemplateService;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;

/// Exception thrown when attempting to create or update an [EntityTemplate] with a name that already exists.
///
/// **Why this exception exists:**
/// - Enforces that entity template names must be unique
/// - Provides domain-specific error information for clear API feedback
/// - Maintains separation of concerns between domain rules and HTTP status codes
///
/// **Usage patterns:**
/// - Thrown from [EntityTemplateService] when duplicate names detected
/// - Caught by [ApiExceptionHandler] and mapped to HTTP 409 status
/// - Contains specific name that caused the conflict for debugging
public class EntityTemplateNameAlreadyExistsException extends RuntimeException {

    public EntityTemplateNameAlreadyExistsException(String name) {
        super(String.format(TEMPLATE_NAME_ALREADY_EXISTS, name));
    }
}
