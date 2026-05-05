package com.decathlon.idp_core.domain.exception.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_VALIDATION_FAILED;

import java.util.List;

import lombok.Getter;

/// Domain exception for entity schema validation failures
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to create an entity, or update an entity, with property values that
///  do not conform to the validation rules defined in the entity's template.
///  This includes violations of required properties, type mismatches, and template rules
/// This enforces the business invariant that entities must conform to the validation
/// rules defined in their template's property definitions and relation constraints.
///
/// **Why this exception exists:**
/// - Enforces business constraint that entity operations require valid property values
///  that conform to template rules
/// - Provides domain-specific error information for API responses
/// - Maintains template-entity relationship integrity
@Getter
public class EntityValidationException extends RuntimeException {

    /**
     * -- GETTER --
     * Returns the list of individual validation violation messages.
     * ///
     * ///
     * @return immutable list of violation messages
     */
    private final List<String> violations;

    /// Constructs a new exception with a list of validation violation messages.
    ///
    /// @param violations the list of validation error messages
    public EntityValidationException(List<String> violations) {
        super(ENTITY_VALIDATION_FAILED + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

}
