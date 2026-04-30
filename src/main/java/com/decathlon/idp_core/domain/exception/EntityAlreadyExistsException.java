package com.decathlon.idp_core.domain.exception;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_ALREADY_EXISTS;

import com.decathlon.idp_core.domain.model.entity.Entity;

/// Domain exception for duplicate [Entity] business entities within a template scope.
public class EntityAlreadyExistsException extends RuntimeException {

    /// Constructs a new exception with template and entity identifiers.
    ///
    /// @param templateIdentifier the identifier of the template
    /// @param entityName the duplicate entity name
    public EntityAlreadyExistsException(String templateIdentifier, String entityName) {
        super(String.format(ENTITY_ALREADY_EXISTS, templateIdentifier, entityName));
    }
}
