package com.decathlon.idp_core.domain.service.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_VALUE_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_IDENTIFIERS_NOT_NULL;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
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
    private final EntityTemplateRepositoryPort entityTemplateRepository;
    private final PropertyValidationService propertyValidationService;

    /// Check entity template existence to ensure valid template reference before deeper validations.
    /// @param entity the entity whose template existence is to be checked
    /// @throws EntityTemplateNotFoundException if the template referenced by the entity does not exist
    void checkTemplateExist(final String entity) {
        if (!entityTemplateRepository.existsByIdentifier(entity)) {
            throw new EntityTemplateNotFoundException("identifier", entity);
        }
    }

    /// Validates intrinsic entity data integrity and template-driven rules.
    ///
    /// @param entity the entity to validate
    /// @throws EntityValidationException       when one or more validation rules are violated
    /// @throws EntityAlreadyExistsException    if an entity with the same identifier exists for the template
    /// @throws EntityTemplateNotFoundException if the referenced template does not exist
    void validateEntity(Entity entity) {
        checkEntityAlreadyExist(entity);
        EntityTemplate template = entityTemplateRepository.findByIdentifier(entity.templateIdentifier())
                .orElseThrow(() -> new EntityTemplateNotFoundException("identifier", entity.templateIdentifier()));

        Violations violations = new Violations();

        validateEntityHeader(entity, violations);
        validatePropertiesShape(entity.properties(), violations);
        validateRelationsShape(entity.relations(), violations);
        validateAgainstTemplate(template, entity.properties(), violations);

        if (!violations.isEmpty()) {
            throw new EntityValidationException(violations.asList());
        }
    }

    private void validateEntityHeader(Entity entity, Violations violations) {
        violations.addIfBlank(entity.name(), ENTITY_NAME_MANDATORY);
        violations.addIfBlank(entity.identifier(), ENTITY_IDENTIFIER_MANDATORY);
    }

    private void validatePropertiesShape(List<Property> properties, Violations violations) {
        if (properties == null) {
            return;
        }
        for (int i = 0; i < properties.size(); i++) {
            Property prop = properties.get(i);
            if (prop.name() == null || prop.name().isBlank()) {
                violations.addIndexed("Property", i, PROPERTY_NAME_MANDATORY);
            }
            if (prop.value() == null || prop.value().isBlank()) {
                violations.addIndexed("Property", i, PROPERTY_VALUE_MANDATORY);
            }
        }
    }

    private void validateRelationsShape(List<Relation> relations, Violations violations) {
        if (relations == null) {
            return;
        }
        for (int i = 0; i < relations.size(); i++) {
            Relation rel = relations.get(i);
            if (rel.name() == null || rel.name().isBlank()) {
                violations.addIndexed("Relation", i, RELATION_NAME_MANDATORY_SIMPLE);
            }
            if (rel.targetEntityIdentifiers() == null) {
                violations.addIndexed("Relation", i, RELATION_TARGET_IDENTIFIERS_NOT_NULL);
            }
        }
    }

    /// Validates entity properties against the template's property definitions, enforcing required fields and value rules.
    /// @param template the entity template whose property definitions are used for validation
    /// @param properties the list of properties from the entity to validate
    /// @param violations the accumulator for validation violation messages
    private void validateAgainstTemplate(EntityTemplate template,
                                         List<Property> properties,
                                         Violations violations) {
        List<PropertyDefinition> definitions = Optional.ofNullable(template.propertiesDefinitions()).orElse(List.of());
        Map<String, Property> propertiesByName = Optional.ofNullable(properties).orElse(List.of()).stream()
                .filter(p -> p.name() != null)
                .collect(Collectors.toMap(Property::name, p -> p, (left, _) -> left));

        for (PropertyDefinition definition : definitions) {
            Property property = propertiesByName.get(definition.name());
            boolean missing = property == null || property.value() == null || property.value().isBlank();

            if (missing) {
                if (definition.required()) {
                    violations.add(PROPERTY_REQUIRED_MISSING, definition.name(), template.identifier());
                }
                continue;
            }

            propertyValidationService
                    .validatePropertyValue(definition, property.value(), property.rawValue())
                    .forEach(violations::add);
        }
    }

    /// Checks for existing entity with same template and identifier to prevent duplicates.
    /// @param entity the entity to check for existence
    /// @throws EntityAlreadyExistsException if an entity with the same template and identifier already exists
    void checkEntityAlreadyExist(final Entity entity) {
        if (entity.identifier() != null
                && entityRepository
                        .findByTemplateIdentifierAndIdentifier(entity.templateIdentifier(), entity.identifier())
                        .isPresent()) {
            throw new EntityAlreadyExistsException(entity.templateIdentifier(), entity.identifier());
        }
    }
}
