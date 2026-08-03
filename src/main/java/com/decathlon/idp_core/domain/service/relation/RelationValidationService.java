package com.decathlon.idp_core.domain.service.relation;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_ENTITY_RELATIONS_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NOT_DEFINED_IN_TEMPLATE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_ENTITY_NOT_FOUND;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_TEMPLATE_MISMATCH;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TOO_MANY_TARGETS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoRelationsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity.Violations;

import lombok.RequiredArgsConstructor;

/// Domain service validating entity relations against template relation definitions.
///
/// This service provides two levels of validation:
/// 1. **Mapping validation** ([#validateMappingRelationsAgainstTemplate]): validates that
///    relation names exist in the template and required relations are mapped.
///    Used by dynamic mapping validation.
/// 2. **Full relation validation** ([#validateRelationsAgainstTemplate]): validates complete
///    [Relation] objects including cardinality and target existence. Used by entity validation.
@Service
@RequiredArgsConstructor
public class RelationValidationService {

  private final EntityRepositoryPort entityRepository;

  /// Validates mapping relation names against the template's relation
  /// definitions.
  ///
  /// This is the **unified validation method for dynamic mappings**, similar to
  /// `PropertyValidationService.validateAgainstTemplate()`.
  ///
  /// **Validations performed:**
  /// 1. All relation names must exist in the template
  /// 2. All required relations must have mappings
  ///
  /// **Fail-fast:** Throws on the first validation error encountered.
  ///
  /// @param template the target template whose relation definitions are the
  /// source of truth
  /// @param mappedRelationNames the relation names that have mappings
  /// @throws RelationNameNotFoundEntityTemplateRelationsException when a name is
  /// not declared
  /// in the template
  /// @throws EntityDynamicMappingHasNoRelationsException when required relations
  /// are missing
  public void validateMappingRelationsAgainstTemplate(EntityTemplate template,
      List<String> mappedRelationNames) {
    validateNamesExistInTemplate(template, mappedRelationNames);
    validateRequiredRelationsAreMapped(template, mappedRelationNames);
  }

  /// Validates that all relation names in the provided list exist in the
  /// template.
  ///
  /// **Fail-fast:** Throws on the first unknown relation name encountered.
  private void validateNamesExistInTemplate(EntityTemplate template, List<String> relationNames) {
    if (relationNames == null || relationNames.isEmpty()) {
      return;
    }

    Set<String> definedRelationNames = getDefinedRelationNames(template);

    relationNames.stream().filter(name -> !definedRelationNames.contains(name)).findFirst()
        .ifPresent(name -> {
          throw new RelationNameNotFoundEntityTemplateRelationsException(
              String.format(RELATION_NOT_DEFINED_IN_TEMPLATE, name, template.identifier()));
        });
  }

  /// Validates that all required relation definitions in the template are mapped.
  private void validateRequiredRelationsAreMapped(EntityTemplate template,
      List<String> mappedRelationNames) {
    List<RelationDefinition> definitions = template.relationsDefinitions() != null
        ? template.relationsDefinitions()
        : List.of();

    List<String> mappedNames = mappedRelationNames != null ? mappedRelationNames : List.of();

    List<String> missingRelations = definitions.stream().filter(RelationDefinition::required)
        .map(RelationDefinition::name).filter(requiredName -> !mappedNames.contains(requiredName))
        .toList();

    if (!missingRelations.isEmpty()) {
      throw new EntityDynamicMappingHasNoRelationsException(
          String.format(ENTITY_DYNAMIC_MAPPING_ENTITY_RELATIONS_MISSING, missingRelations));
    }
  }

  private Set<String> getDefinedRelationNames(EntityTemplate template) {
    if (template.relationsDefinitions() == null) {
      return Set.of();
    }
    return template.relationsDefinitions().stream().map(RelationDefinition::name)
        .collect(Collectors.toSet());
  }

  /// Validates entity relations against the template's relation definitions,
  /// enforcing required relations and cardinality constraints.
  /// @param template the entity template whose relation definitions are used for
  /// validation
  /// @param providedRelations the actual relations provided in the entity to
  /// validate
  /// @param violations the accumulator for any validation violations found during
  /// the process
  public void validateRelationsAgainstTemplate(EntityTemplate template,
      List<Relation> providedRelations, Violations violations) {

    List<RelationDefinition> definitions = template.relationsDefinitions() != null
        ? template.relationsDefinitions()
        : List.of();
    List<Relation> relations = providedRelations != null ? providedRelations : List.of();

    Map<String, RelationDefinition> definitionsByName = definitions.stream()
        .filter(def -> def.name() != null).collect(Collectors.toMap(RelationDefinition::name,
            def -> def, (existing, replacement) -> existing));

    Map<String, Relation> relationsByName = relations.stream().filter(rel -> rel.name() != null)
        .collect(Collectors.toMap(Relation::name, rel -> rel, (existing, replacement) -> existing));

    for (Relation relation : relations) {
      if (relation.name() != null && !definitionsByName.containsKey(relation.name())) {
        violations.add(RELATION_NOT_DEFINED_IN_TEMPLATE, relation.name(), template.identifier());
      } else {
        validateRelationTargetEntityExistence(relation, violations);
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

  /// Validates that all target entity identifiers in the relation actually exist
  /// in the database and match the expected target template.
  ///
  /// @param relation the relation whose target entity identifiers are to be
  /// validated
  /// @param violations the accumulator for any validation violations found during
  /// the process
  private void validateRelationTargetEntityExistence(Relation relation, Violations violations) {
    List<String> targetIdentifiers = extractValidTargetIdentifiers(relation);

    if (targetIdentifiers.isEmpty()) {
      return;
    }

    var existingEntities = entityRepository.findByIdentifierIn(targetIdentifiers);
    Set<String> existingIdentifiers = existingEntities.stream().map(EntitySummary::identifier)
        .collect(Collectors.toSet());

    for (String identifier : targetIdentifiers) {
      if (!existingIdentifiers.contains(identifier)) {
        violations.add(RELATION_TARGET_ENTITY_NOT_FOUND, relation.name(), identifier);
      }
    }

    // Validate that target entities match the expected target template
    validateRelationTargetTemplates(relation, existingEntities, violations);
  }

  /// Validates that all target entities have the expected target template
  /// identifier
  /// as defined in the relation.
  ///
  /// @param relation the relation with target template identifier
  /// @param existingEntities the existing entities to validate
  /// @param violations the accumulator for validation violations
  private void validateRelationTargetTemplates(Relation relation,
      List<EntitySummary> existingEntities, Violations violations) {
    if (relation.targetTemplateIdentifier() == null || existingEntities.isEmpty()) {
      return;
    }

    for (EntitySummary entity : existingEntities) {
      if (extractValidTargetIdentifiers(relation).contains(entity.identifier())
          && !relation.targetTemplateIdentifier().equals(entity.templateIdentifier())) {
        violations.add(RELATION_TARGET_TEMPLATE_MISMATCH, relation.name(),
            relation.targetTemplateIdentifier(), entity.identifier(), entity.templateIdentifier());
      }
    }
  }

  /// Extracts non-null, non-blank target entity identifiers from the relation,
  /// returning an empty list if the relation or its target identifiers are null.
  ///
  /// @param relation the relation from which to extract target entity identifiers
  /// @return a list of valid target entity identifiers; empty if none are valid
  /// or if the relation is null
  private List<String> extractValidTargetIdentifiers(Relation relation) {
    if (relation == null || relation.targetEntityIdentifiers() == null) {
      return List.of();
    }
    return relation.targetEntityIdentifiers().stream().filter(id -> id != null && !id.isBlank())
        .toList();
  }
}
