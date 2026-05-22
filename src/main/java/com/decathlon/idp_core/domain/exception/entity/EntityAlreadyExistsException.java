package com.decathlon.idp_core.domain.exception.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_ALREADY_EXISTS;

import com.decathlon.idp_core.domain.model.entity.Entity;

/// Domain exception for duplicate [Entity] business entities within the same template context.
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to create an Entity that already exist within a specific template context.
/// This enforces the business invariant that entities must be unique within their template context.
///
/// **Why this exception exists:**
/// - Enforces business constraint that entity operations require unique entities within a template context
/// - Provides domain-specific error information for API responses
/// - Maintains template-entity relationship integrity
public class EntityAlreadyExistsException extends RuntimeException {

    /// Constructs a new exception with template and entity identifiers.
    ///
    /// @param templateIdentifier the identifier of the template
    /// @param entityName the duplicate entity name
    public EntityAlreadyExistsException(String templateIdentifier, String entityName) {
        super(String.format(ENTITY_ALREADY_EXISTS, entityName, templateIdentifier));
    }
}
