package com.decathlon.idp_core.domain.exception;

/// Domain exception for missing [Entity] business entities.
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to access an Entity that doesn't exist within a specific template context.
/// This enforces the business invariant that entities must exist before operations.
///
/// **Why this exception exists:**
/// - Enforces business constraint that entity operations require existing entities
/// - Provides domain-specific error information for API responses
/// - Maintains template-entity relationship integrity
public class EntityNotFoundException extends RuntimeException {

    /// Constructs a new exception with template and entity identifiers.
    ///
    /// **Why this exists:** Provides standardized error message format that includes
    /// both template and entity context for clear debugging and API error responses.
    ///
    /// @param templateIdentifier the identifier of the template
    /// @param entityIdentifier the identifier of the entity
    public EntityNotFoundException(String templateIdentifier, String entityIdentifier) {
        super(String.format("Entity not found with template identifier %s and entity identifier '%s'", templateIdentifier, entityIdentifier));
    }

}
