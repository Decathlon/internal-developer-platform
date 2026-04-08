package com.decathlon.idp_core.domain.exception;

/**
 * Exception thrown when an entity is not found for a given template identifier and entity identifier.
 * Typically used in service or controller layers to indicate a missing entity resource.
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * Constructs a new EntityNotFoundException with the specified template and entity identifiers.
     *
     * @param templateIdentifier the identifier of the template
     * @param entityIdentifier the identifier of the entity
     */
    public EntityNotFoundException(String templateIdentifier, String entityIdentifier) {
        super(String.format("Entity not found with template identifier %s and entity identifier '%s'", templateIdentifier, entityIdentifier));
    }

}
