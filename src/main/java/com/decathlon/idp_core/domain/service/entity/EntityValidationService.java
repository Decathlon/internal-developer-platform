package com.decathlon.idp_core.domain.service.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NOT_DEFINED_IN_TEMPLATE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TOO_MANY_TARGETS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;

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

        for (PropertyDefinition definition : definitions) {
            Property property = propertiesByName.get(definition.name());
            boolean missing = property == null
                    || property.value() == null
                    || (property.value().isBlank());

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

        validateRelationsAgainstTemplate(template, entity.relations(), violations);

        if (!violations.isEmpty()) {
            throw new EntityValidationException(violations.asList());
        }
    }

    /// Validates entity relations against the template's relation definitions, enforcing required relations and cardinality constraints.
    /// @param template the entity template whose relation definitions are used for validation
    /// @param providedRelations the actual relations provided in the entity to validate
    /// @param violations the accumulator for any validation violations found during the process
    private void validateRelationsAgainstTemplate(EntityTemplate template,
                                                  List<Relation> providedRelations,
                                                  Violations violations) {

        List<RelationDefinition> definitions = template.relationsDefinitions() != null ? template.relationsDefinitions() : List.of();
        List<Relation> relations = providedRelations != null ? providedRelations : List.of();

        Map<String, RelationDefinition> definitionsByName = definitions.stream()
                .filter(def -> def.name() != null)
                .collect(Collectors.toMap(RelationDefinition::name, def -> def,
                        (existing, replacement) -> existing));

        Map<String, Relation> relationsByName = relations.stream()
                .filter(rel -> rel.name() != null)
                .collect(Collectors.toMap(Relation::name, rel -> rel,
                        (existing, replacement) -> existing));

        for (Relation relation : relations) {
            if (relation.name() != null && !definitionsByName.containsKey(relation.name())) {
                violations.add(RELATION_NOT_DEFINED_IN_TEMPLATE, relation.name(), template.identifier());
            }
        }

        for (RelationDefinition definition : definitions) {
            Relation relation = relationsByName.get(definition.name());
            List<String> validTargets = extractValidTargetIdentifiers(relation);

            if (definition.required() && validTargets.isEmpty()) {
                violations.add(RELATION_REQUIRED_MISSING, definition.name(), template.identifier());
            }

            if (relation != null && !definition.toMany() && validTargets.size() > 1) {
                violations.add(RELATION_TOO_MANY_TARGETS, definition.name(), template.identifier());
            }
        }
    }

    private List<String> extractValidTargetIdentifiers(Relation relation) {
        if (relation == null || relation.targetEntityIdentifiers() == null) {
            return List.of();
        }
        return relation.targetEntityIdentifiers().stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();
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
