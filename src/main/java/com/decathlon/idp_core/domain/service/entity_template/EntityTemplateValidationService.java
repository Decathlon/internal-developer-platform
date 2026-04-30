package com.decathlon.idp_core.domain.service.entity_template;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import lombok.RequiredArgsConstructor;

/// Domain service to centralize all functional validation rules for [EntityTemplate] operations.
///
/// **Key responsibilities:**
/// - Identifier and name uniqueness enforcement for create and update operations
/// - Property-rule compatibility validation (type vs. rule constraints) delegated to [PropertyDefinitionValidationService]
/// - Template existence verification before deletion
@Service
@RequiredArgsConstructor
public class EntityTemplateValidationService {

    private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;
    private final PropertyDefinitionValidationService propertyDefinitionValidationService;

    /// Validates all business rules before creating a new entity template.
    ///
    /// **Business rules enforced:**
    /// - If `identifier` is provided it must not already exist in the system.
    /// - If `name` is provided it must not already exist in the system.
    /// - Property rules must be compatible with their declared property type.
    ///
    /// @param entityTemplate the template candidate to validate
    /// @throws EntityTemplateAlreadyExistsException when identifier is already taken
    /// @throws EntityTemplateNameAlreadyExistsException when name is already taken
    public void validateForCreate(EntityTemplate entityTemplate) {
        if (entityTemplate.identifier() != null) {
            validateIdentifierUniqueness(entityTemplate.identifier());
        }
        if (entityTemplate.name() != null) {
            validateNameUniqueness(entityTemplate.name());
        }
        validatePropertyRules(entityTemplate);
    }

    /// Validates all business rules before persisting an updated entity template.
    ///
    /// **Business rules enforced:**
    /// - If the identifier changed, the new value must not collide with another template.
    /// - If the name changed, the new value must not collide with another template.
    /// - Property rules in the merged template must be compatible with their declared type.
    ///
    /// @param currentIdentifier the identifier of the template being replaced
    /// @param existingName the current name of the template being replaced
    /// @param mergedTemplate the fully-merged template carrying the desired state
    /// @throws EntityTemplateAlreadyExistsException when the new identifier is already taken
    /// @throws EntityTemplateNameAlreadyExistsException when the new name is already taken
    public void validateForUpdate(String currentIdentifier, String existingName, EntityTemplate mergedTemplate) {
        if (!currentIdentifier.equals(mergedTemplate.identifier())) {
            validateIdentifierUniqueness(mergedTemplate.identifier());
        }
        if (mergedTemplate.name() != null && !Objects.equals(existingName, mergedTemplate.name())) {
            validateNameUniqueness(mergedTemplate.name());
        }
        validatePropertyRules(mergedTemplate);
    }

    /// Validates that a template identifier is non-null and refers to an existing template.
    ///
    /// @param identifier the identifier of the template to delete
    /// @throws IllegalArgumentException when `identifier` is null
    /// @throws EntityTemplateNotFoundException when no template matches `identifier`
    public void validateForDelete(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Template identifier must not be null");
        }
        if (!entityTemplateRepositoryPort.existsByIdentifier(identifier)) {
            throw new EntityTemplateNotFoundException("identifier", identifier);
        }
    }

    /// Checks that no other template already uses the given identifier.
    ///
    /// @param identifier the identifier to check for uniqueness
    /// @throws EntityTemplateAlreadyExistsException when identifier is already taken
    private void validateIdentifierUniqueness(String identifier) {
        if (entityTemplateRepositoryPort.existsByIdentifier(identifier)) {
            throw new EntityTemplateAlreadyExistsException(identifier);
        }
    }

    /// Checks that no other template already uses the given name.
    ///
    /// @param name the name to check for uniqueness
    /// @throws EntityTemplateNameAlreadyExistsException when name is already taken
    private void validateNameUniqueness(String name) {
        if (entityTemplateRepositoryPort.existsByName(name)) {
            throw new EntityTemplateNameAlreadyExistsException(name);
        }
    }

    private void validatePropertyRules(EntityTemplate entityTemplate) {
        if (entityTemplate.propertiesDefinitions() == null) {
            return;
        }
        for (PropertyDefinition property : entityTemplate.propertiesDefinitions()) {
            propertyDefinitionValidationService.validatePropertyDefinitionRules(property);
        }
    }

}
