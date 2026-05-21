package com.decathlon.idp_core.domain.service.entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;
import com.decathlon.idp_core.domain.service.relation.RelationValidationService;

import lombok.AllArgsConstructor;

/// Domain validator for [Entity] aggregates.
///
/// Validation pipeline:
/// 1. Existence checks (template found, entity not duplicated).
/// 2. Syntactic checks on the entity itself (name/identifier, nested properties, relations).
/// 3. Template-driven semantic checks (required, type, rules).
@Service
@AllArgsConstructor
public class EntityValidationService {

    private final EntityRepositoryPort entityRepository;
    private final PropertyValidationService propertyValidationService;
    private final RelationValidationService relationValidationService;

     /// Validates intrinsic entity data integrity and template-driven rules.
    ///
    /// **Contract:** the caller is responsible for resolving the [EntityTemplate]
    /// (typically via [com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort])
    /// and passing it in. This avoids a redundant database round-trip and clarifies
    /// the dependency graph of the validation service.
    ///
    /// @param entity the entity to validate
    /// @param template the already-resolved template the entity must conform to
    /// @throws EntityValidationException       when one or more validation rules are violated
    /// @throws EntityAlreadyExistsException    if an entity with the same identifier exists for the template
    void validateForCreation(Entity entity, EntityTemplate template) {
        validateUniqueness(entity);
        validateAgainstTemplate(template, entity);
    }

    /// Validates entity data for update operations.
    ///
    /// **Contract:** update keeps the existing aggregate identity and applies the
    /// same template conformance rules as creation. Uniqueness check is not needed
    /// when updating an already identified entity.
    ///
    /// @param entity the entity payload to validate
    /// @param template the already-resolved template the entity must conform to
    /// @throws EntityValidationException when one or more validation rules are violated
    void validateForUpdate(Entity entity, EntityTemplate template) {
        validateAgainstTemplate(template, entity.properties());
    }

    /// Validates entity properties against the template's property definitions, enforcing required fields and value rules.
    /// @param template the entity template whose property definitions are used for validation
    /// @param entity the entity being validated, containing the actual property values to check
    /// @throws EntityValidationException if any property validation rules are violated, including missing required properties
    private void validateAgainstTemplate(EntityTemplate template,
                                         Entity entity) {
        Violations violations = new Violations();

        List<PropertyDefinition> definitions = Optional.ofNullable(template.propertiesDefinitions()).orElse(List.of());

        Map<String, Property> propertiesByName = Optional.ofNullable(entity.properties()).orElse(List.of()).stream()
                .filter(p -> p.name() != null)
                .collect(Collectors.toMap(Property::name, p -> p, (left, _) -> left));

        propertyValidationService.validatePropertiesAgainstTemplate(template, definitions, propertiesByName, violations);

        relationValidationService.validateRelationsAgainstTemplate(template, entity.relations(), violations);

        if (!violations.isEmpty()) {
            throw new EntityValidationException(violations.asList());
        }
    }


    /// Checks for existing entity with same template and identifier to prevent duplicates.
    /// @param entity the entity to check for existence
    /// @throws EntityAlreadyExistsException if an entity with the same template and identifier already exists
   private void validateUniqueness(final Entity entity) {
        if (entity.identifier() != null
                && entityRepository
                        .findByTemplateIdentifierAndIdentifier(entity.templateIdentifier(), entity.identifier())
                        .isPresent()) {
            throw new EntityAlreadyExistsException(entity.templateIdentifier(), entity.identifier());
        }
    }
}
