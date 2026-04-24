package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_VALIDATION_FAILED;

import java.util.List;

import lombok.Getter;

/// Domain exception for entity schema validation failures
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
