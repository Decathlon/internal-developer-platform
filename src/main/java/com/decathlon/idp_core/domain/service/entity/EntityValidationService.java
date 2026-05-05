package com.decathlon.idp_core.domain.service.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_REQUIRED_MISSING;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;

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
    void validateEntity(Entity entity, EntityTemplate template) {
        Violations violations = new Violations();
        validateAgainstTemplate(template, entity.properties(), violations);

        if (!violations.isEmpty()) {
            throw new EntityValidationException(violations.asList());
        }
    }

    /// Validates entity properties against the template's property definitions, enforcing required fields and value rules.
    /// @param template the entity template whose property definitions are used for validation
    /// @param properties the list of properties from the entity to validate
    /// @param violations the accumulator for validation v  iolation messages
    private void validateAgainstTemplate(EntityTemplate template,
                                         List<Property> properties,
                                         Violations violations) {
        List<PropertyDefinition> definitions = Optional.ofNullable(template.propertiesDefinitions()).orElse(List.of());
        Map<String, Property> propertiesByName = Optional.ofNullable(properties).orElse(List.of()).stream()
                .filter(p -> p.name() != null)
                .collect(Collectors.toMap(Property::name, p -> p, (left, _) -> left));

        for (PropertyDefinition definition : definitions) {
            Property property = propertiesByName.get(definition.name());
            boolean missing = property == null
                    || property.value() == null
                    || (property.value() instanceof String s && s.isBlank());

            if (missing) {
                if (definition.required()) {
                    violations.add(PROPERTY_REQUIRED_MISSING, definition.name(), template.identifier());
                }
                continue;
            }

            propertyValidationService
                    .validatePropertyValue(definition, property.value())
                    .forEach(violations::add);
        }
    }

    /// Checks for existing entity with same template and identifier to prevent duplicates.
    /// @param entity the entity to check for existence
    /// @throws EntityAlreadyExistsException if an entity with the same template and identifier already exists
    void checkUniqueness(final Entity entity) {
        if (entity.identifier() != null
                && entityRepository
                        .findByTemplateIdentifierAndIdentifier(entity.templateIdentifier(), entity.identifier())
                        .isPresent()) {
            throw new EntityAlreadyExistsException(entity.templateIdentifier(), entity.identifier());
        }
    }
}
