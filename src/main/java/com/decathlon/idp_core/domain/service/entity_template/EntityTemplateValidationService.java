package com.decathlon.idp_core.domain.service.entity_template;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIdentifierCannotChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationCannotTargetItselfException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationTargetTemplateChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.TargetTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import lombok.RequiredArgsConstructor;

/// Domain service to centralize all functional validation rules for [EntityTemplate] operations.
///
/// **Key responsibilities:**
/// - Identifier and name uniqueness enforcement for create and update operations
/// - Property-rule compatibility validation (type vs. rule constraints) delegated to [PropertyDefinitionValidationService]
/// - Relation definitions validation (name uniqueness and referential integrity) delegated to [RelationDefinitionValidationService]
/// - Template existence verification before deletion
@Service
@RequiredArgsConstructor
public class EntityTemplateValidationService {

    private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;
    private final PropertyDefinitionValidationService propertyDefinitionValidationService;
    private final RelationDefinitionValidationService relationDefinitionValidationService;

    /// Validates all business rules before creating a new entity template.
    ///
    /// **Business rules enforced:**
    /// - If `identifier` is provided it must not already exist in the system.
    /// - If `name` is provided it must not already exist in the system.
    /// - Property rules must be compatible with their declared property type.
    /// - Relation names must be unique within the template.
    /// - No relation may target the template's own identifier (no self-reference).
    /// - All target templates referenced by relations must exist in the system.
    ///
    /// @param entityTemplate the template candidate to validate
    /// @throws EntityTemplateAlreadyExistsException when identifier is already taken
    /// @throws EntityTemplateNameAlreadyExistsException when name is already taken
    /// @throws PropertyDefinitionRulesConflictException when rules violate business invariants
    /// @throws PropertyNameAlreadyExistsException if duplicate property names are found
    /// @throws RelationNameAlreadyExistsException if duplicate relation names are found
    /// @throws RelationCannotTargetItselfException when a relation targets the template itself
    public void validateForCreation(EntityTemplate entityTemplate) {
        validateIdentifierUniqueness(entityTemplate.identifier());
        validateNameUniqueness(entityTemplate.name());
        if (entityTemplate.propertiesDefinitions() != null) {
            propertyDefinitionValidationService.validatePropertyNamesUniqueness(entityTemplate.propertiesDefinitions());
            validateTemplateProperties(entityTemplate);
        }
        if (entityTemplate.relationsDefinitions() != null) {
            relationDefinitionValidationService.validateRelationNamesUniqueness(entityTemplate.relationsDefinitions());
            validateTemplateRelations(entityTemplate);
        }
    }

    /// Validates all business rules before persisting an updated entity template.
    ///
    /// **Business rules enforced:**
    /// - If the identifier changed, the new value must not collide with another template.
    /// - If the name changed, the new value must not collide with another template.
    /// - Property rules in the merged template must be compatible with their declared type.
    /// - Relation names must be unique within the template.
    /// - No relation may target the template's own identifier (no self-reference).
    /// - All target templates referenced by relations must exist in the system.
    /// - Relation target template identifiers cannot be changed after creation.
    ///
    /// @param currentIdentifier the identifier of the template being replaced
    /// @param existingName the current name of the template being replaced
    /// @param existingTemplate the current state of the template being replaced
    /// @param mergedTemplate the fully-merged template carrying the desired state
    /// @throws EntityTemplateAlreadyExistsException when the new identifier is already taken
    /// @throws EntityTemplateNameAlreadyExistsException when the new name is already taken
    /// @throws PropertyDefinitionRulesConflictException when rules violate business invariants
    /// @throws PropertyNameAlreadyExistsException if duplicate property names are found
    /// @throws RelationNameAlreadyExistsException if duplicate relation names are found
    /// @throws RelationTargetTemplateChangeException when a relation target template is changed
    /// @throws RelationCannotTargetItselfException when a relation targets the template itself
    public void validateForUpdate(String currentIdentifier, String existingName, EntityTemplate existingTemplate, EntityTemplate mergedTemplate) {
        if (!currentIdentifier.equals(mergedTemplate.identifier())) {
            throw new EntityTemplateIdentifierCannotChangeException(mergedTemplate.identifier());
        }
        if (!Objects.equals(existingName, mergedTemplate.name())) {
            validateNameUniqueness(mergedTemplate.name());
        }
        if (mergedTemplate.propertiesDefinitions() != null) {
            propertyDefinitionValidationService.validatePropertyNamesUniqueness(mergedTemplate.propertiesDefinitions());
            propertyDefinitionValidationService.validateTypeChanges(existingTemplate.propertiesDefinitions(), mergedTemplate.propertiesDefinitions());
            validateTemplateProperties(mergedTemplate);
        }
        if (mergedTemplate.relationsDefinitions() != null) {
            relationDefinitionValidationService.validateRelationNamesUniqueness(mergedTemplate.relationsDefinitions());
            relationDefinitionValidationService.validateTargetTemplateChanges(existingTemplate.relationsDefinitions(), mergedTemplate.relationsDefinitions());
            validateTemplateRelations(mergedTemplate);
        }
    }

    /// Validates that a template identifier is non-null and refers to an existing template.
    ///
    /// @param identifier the identifier of the template to delete
    /// @throws EntityTemplateNotFoundException when `identifier` is null
    /// @throws EntityTemplateNotFoundException when no template matches `identifier`
    public void validateForDeletion(String identifier) {
        if (identifier == null) {
            throw new EntityTemplateNotFoundException("identifier", "null");
        }
        validateTemplateExists(identifier);
    }

    /// Checks that the entity template exists.
    ///
    /// @param identifier the identifier to check for existence
    /// @throws EntityTemplateNotFoundException when no template matches `identifier`
    public void validateTemplateExists(String identifier) {
        if (!entityTemplateRepositoryPort.existsByIdentifier(identifier)) {
            throw new EntityTemplateNotFoundException("identifier", identifier);
        }
    }

    /// Checks that no other template already uses the given identifier.
    ///
    /// @param identifier the identifier to check for uniqueness
    /// @throws EntityTemplateAlreadyExistsException when identifier is already taken
    public void validateIdentifierUniqueness(String identifier) {
        if (entityTemplateRepositoryPort.existsByIdentifier(identifier)) {
            throw new EntityTemplateAlreadyExistsException(identifier);
        }
    }

    /// Checks that no other template already uses the given name.
    ///
    /// @param name the name to check for uniqueness
    /// @throws EntityTemplateNameAlreadyExistsException when name is already taken
    public void validateNameUniqueness(String name) {
        if (entityTemplateRepositoryPort.existsByName(name)) {
            throw new EntityTemplateNameAlreadyExistsException(name);
        }
    }

    /// Validates all property definitions within the template for structural and
    /// referential integrity.
    ///
    /// **Contract:** Enforces properties business rules
    /// - Property rules integrity: all rules referenced by properties must
    ///   be valid and coherent based on the property's type
    ///
    /// **Precondition:** propertiesDefinitions must not be null
    ///
    /// @param entityTemplate the template containing properties to validate
    /// @throws PropertyDefinitionRulesConflictException when rules violate business
    ///                                            logic
    private void validateTemplateProperties(EntityTemplate entityTemplate) {
        for (PropertyDefinition property : entityTemplate.propertiesDefinitions()) {
            propertyDefinitionValidationService.validatePropertyDefinitionRules(property);
        }
    }

    /// Validates all relation definitions within the template for structural and
    /// referential integrity.
    ///
    /// **Contract:** Enforces relation business rules
    /// - No relation may target the template's own identifier
    /// - Referential integrity: all target templates referenced by relations must
    ///   exist in the system
    ///
    /// **Precondition:** relationsDefinitions must not be null
    ///
    /// @param entityTemplate the template containing relations to validate
    /// @throws TargetTemplateNotFoundException    if any referenced target template
    /// @throws RelationCannotTargetItselfException if a relation targets the template itself
    ///                                            doesn't exist
    private void validateTemplateRelations(EntityTemplate entityTemplate) {
        relationDefinitionValidationService.validateRelationNoSelfReference(entityTemplate.identifier(), entityTemplate.relationsDefinitions());
        relationDefinitionValidationService.validateTargetTemplatesExist(entityTemplate.relationsDefinitions());
    }

}
