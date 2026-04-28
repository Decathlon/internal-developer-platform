package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IDENTIFIER_NOT_FOUND;

import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;

/// Exception thrown when a relation references a non-existent target template.
///
/// This exception represents a business rule violation where relations must reference
/// valid, existing entity templates.
///
/// **Why this exception exists:**
/// - Enforces referential integrity: relations must point to valid target templates
/// - Provides domain-specific error information for clear API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class TargetTemplateNotFoundException extends RuntimeException {

    /// Constructs a new exception with the target template identifier that was not found.
    ///
    /// @param targetTemplateIdentifier the identifier of the target template that doesn't exist
    public TargetTemplateNotFoundException(String targetTemplateIdentifier) {
        super(String.format(TEMPLATE_IDENTIFIER_NOT_FOUND, targetTemplateIdentifier));
    }
}
