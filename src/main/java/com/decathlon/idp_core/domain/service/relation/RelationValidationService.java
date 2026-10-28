package com.decathlon.idp_core.domain.service.relation;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_ENTITY_RELATIONS_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NOT_DEFINED_IN_TEMPLATE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_ENTITY_NOT_FOUND;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_TEMPLATE_MISMATCH;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TOO_MANY_TARGETS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
/// 2. **Full relation validation**
///    ([#validateAndNormalizeRelationsAgainstTemplate]): validates [Relation]
///    objects, removes unresolved optional targets, and checks cardinality and
///    target templates. Used by entity write validation.
@Service
@RequiredArgsConstructor
public class RelationValidationService {

  private record RelationValidationOptions(boolean skipDatabaseChecks,
      boolean omitMissingOptionalTargets) {
  }

  private record RelationNormalization(Optional<Relation> relation,
      List<String> targetIdentifiers) {
  }

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
    if (template.relationsDefinitions().isEmpty()) {
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
    validateRelationsAgainstTemplate(template, providedRelations, violations, false, false);
  }

  /// Validates relations and removes unresolved targets from optional relations.
  ///
  /// Required relations and targets with an incompatible template remain
  /// validation errors. The returned relations contain only resolved targets
  /// for optional relations.
  ///
  /// @param template the entity template whose relation definitions are used
  /// @param providedRelations the relations to validate and normalize
  /// @param violations the accumulator for validation violations
  /// @return normalized relations suitable for persistence
  public List<Relation> validateAndNormalizeRelationsAgainstTemplate(EntityTemplate template,
      List<Relation> providedRelations, Violations violations) {
    return validateRelationsAgainstTemplate(template, providedRelations, violations, false, true);
  }

  /// Internal unified relation validation used by both normal and dry-run
  /// workflows.
  ///
  /// @param skipDatabaseChecks when true, skips target-entity existence checks
  /// in repository-backed validation
  /// @param omitMissingOptionalTargets when true, unresolved targets on optional
  /// relations are removed instead of reported
  /// @return relations after optional target normalization
  private List<Relation> validateRelationsAgainstTemplate(EntityTemplate template,
      List<Relation> providedRelations, Violations violations, boolean skipDatabaseChecks,
      boolean omitMissingOptionalTargets) {

    List<RelationDefinition> definitions = template.relationsDefinitions() != null
        ? template.relationsDefinitions()
        : List.of();
    List<Relation> relations = providedRelations != null ? providedRelations : List.of();

    Map<String, RelationDefinition> definitionsByName = definitions.stream()
        .filter(def -> def.name() != null).collect(Collectors.toMap(RelationDefinition::name,
            def -> def, (existing, replacement) -> existing));

    Map<String, Relation> relationsByName = relations.stream().filter(rel -> rel.name() != null)
        .collect(Collectors.toMap(Relation::name, rel -> rel, (existing, replacement) -> existing));

    Map<String, List<String>> targetIdentifiersByName = new HashMap<>();
    var options = new RelationValidationOptions(skipDatabaseChecks, omitMissingOptionalTargets);
    List<Relation> normalizedRelations = normalizeRelations(template.identifier(), relations,
        definitionsByName, targetIdentifiersByName, violations, options);

    validateRelationDefinitions(template.identifier(), definitions, relationsByName,
        targetIdentifiersByName, violations);
    return normalizedRelations;
  }

  private List<Relation> normalizeRelations(String templateIdentifier, List<Relation> relations,
      Map<String, RelationDefinition> definitionsByName,
      Map<String, List<String>> targetIdentifiersByName, Violations violations,
      RelationValidationOptions options) {
    List<Relation> normalizedRelations = new ArrayList<>();

    for (Relation relation : relations) {
      RelationNormalization normalization = normalizeRelation(templateIdentifier, relation,
          definitionsByName, violations, options);
      if (relation.name() != null && definitionsByName.containsKey(relation.name())) {
        targetIdentifiersByName.putIfAbsent(relation.name(), normalization.targetIdentifiers());
      }
      normalization.relation().ifPresent(normalizedRelations::add);
    }

    return normalizedRelations;
  }

  private RelationNormalization normalizeRelation(String templateIdentifier, Relation relation,
      Map<String, RelationDefinition> definitionsByName, Violations violations,
      RelationValidationOptions options) {
    if (relation.name() != null && !definitionsByName.containsKey(relation.name())) {
      violations.add(RELATION_NOT_DEFINED_IN_TEMPLATE, relation.name(), templateIdentifier);
      return new RelationNormalization(Optional.of(relation),
          extractValidTargetIdentifiers(relation));
    }

    RelationDefinition definition = definitionsByName.get(relation.name());
    List<String> targetIdentifiers = extractValidTargetIdentifiers(relation);
    List<String> normalizedTargetIdentifiers = normalizeTargetIdentifiers(relation, definition,
        targetIdentifiers, violations, options);

    if (omitsEmptyOptionalRelation(definition, normalizedTargetIdentifiers, options)) {
      return new RelationNormalization(Optional.empty(), normalizedTargetIdentifiers);
    }

    Relation normalizedRelation = new Relation(relation.id(), relation.name(),
        relation.targetTemplateIdentifier(), normalizedTargetIdentifiers);
    return new RelationNormalization(Optional.of(normalizedRelation), normalizedTargetIdentifiers);
  }

  private List<String> normalizeTargetIdentifiers(Relation relation, RelationDefinition definition,
      List<String> targetIdentifiers, Violations violations, RelationValidationOptions options) {
    if (options.skipDatabaseChecks() || targetIdentifiers.isEmpty()) {
      return targetIdentifiers;
    }

    var existingEntities = entityRepository.findByIdentifierIn(targetIdentifiers);
    Set<String> existingIdentifiers = existingEntities.stream().map(EntitySummary::identifier)
        .collect(Collectors.toSet());

    validateTargetExistence(relation, definition, targetIdentifiers, existingIdentifiers,
        violations, options);
    validateRelationTargetTemplates(relation, existingEntities, violations);

    if (omitsUnresolvedOptionalTargets(definition, options)) {
      return targetIdentifiers.stream().filter(existingIdentifiers::contains).toList();
    }

    return targetIdentifiers;
  }

  private void validateTargetExistence(Relation relation, RelationDefinition definition,
      List<String> targetIdentifiers, Set<String> existingIdentifiers, Violations violations,
      RelationValidationOptions options) {
    for (String identifier : targetIdentifiers) {
      if (!existingIdentifiers.contains(identifier)
          && !omitsUnresolvedOptionalTargets(definition, options)) {
        violations.add(RELATION_TARGET_ENTITY_NOT_FOUND, relation.name(), identifier);
      }
    }
  }

  private void validateRelationDefinitions(String templateIdentifier,
      List<RelationDefinition> definitions, Map<String, Relation> relationsByName,
      Map<String, List<String>> targetIdentifiersByName, Violations violations) {
    for (RelationDefinition definition : definitions) {
      Relation relation = relationsByName.get(definition.name());
      List<String> validTargets = relation == null
          ? List.of()
          : targetIdentifiersByName.getOrDefault(relation.name(),
              extractValidTargetIdentifiers(relation));

      if (definition.required() && validTargets.isEmpty()) {
        violations.add(RELATION_REQUIRED_MISSING, definition.name(), templateIdentifier);
      }

      if (relation != null && !definition.toMany() && validTargets.size() > 1) {
        violations.add(RELATION_TOO_MANY_TARGETS, definition.name(), templateIdentifier);
      }
    }
  }

  private boolean omitsUnresolvedOptionalTargets(RelationDefinition definition,
      RelationValidationOptions options) {
    return options.omitMissingOptionalTargets() && definition != null && !definition.required();
  }

  private boolean omitsEmptyOptionalRelation(RelationDefinition definition,
      List<String> targetIdentifiers, RelationValidationOptions options) {
    return omitsUnresolvedOptionalTargets(definition, options) && targetIdentifiers.isEmpty();
  }

  /// Validates entity relations for dry-run executions.
  ///
  /// Keeps structural validation (relation names, required relations,
  /// cardinality)
  /// and skips database existence checks for target entity identifiers.
  ///
  /// This method is intentionally additive and does not change the behavior of
  /// [#validateRelationsAgainstTemplate].
  ///
  /// @param template the entity template whose relation definitions are used for
  /// validation
  /// @param providedRelations the actual relations provided by mapping extraction
  /// @param violations the accumulator for any validation violations found during
  /// the process
  public void validateRelationsAgainstTemplateForDryRun(EntityTemplate template,
      List<Relation> providedRelations, Violations violations) {
    validateRelationsAgainstTemplate(template, providedRelations, violations, true, false);
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
