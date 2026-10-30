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
import com.decathlon.idp_core.domain.model.enums.RelationTargetValidationMode;
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
///    ([#validateAndNormalizeRelationsAgainstTemplate]): filters target identifiers
///    not found in the repository from optional relations, omits relations left
///    without targets, and validates required relations, cardinality, and target
///    templates. Used by entity write validation.
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
  private final RelationTargetValidationMode targetValidationMode;

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

    Set<String> definedRelationNames = template.relationsDefinitions().stream()
        .map(RelationDefinition::name).collect(Collectors.toSet());

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

  /// Validates relations and filters out target identifiers that do not resolve
  /// to entities in the repository from optional relations. Omits an optional
  /// relation if no targets remain.
  ///
  /// Missing targets on required relations and existing targets with an
  /// incompatible template remain validation errors. The returned relations
  /// retain all resolved targets.
  ///
  /// @param template the entity template whose relation definitions are used
  /// @param providedRelations the relations and target identifiers to validate
  /// @param violations the accumulator for validation violations
  /// @return relations with repository-missing target identifiers filtered out
  /// and optional relations with no targets omitted
  public List<Relation> validateAndNormalizeRelationsAgainstTemplate(EntityTemplate template,
      List<Relation> providedRelations, Violations violations) {
    boolean omitMissingOptionalTargets = targetValidationMode == RelationTargetValidationMode.TOLERANT;
    return validateRelationsAgainstTemplate(template, providedRelations, violations, false,
        omitMissingOptionalTargets);
  }

  /// Validates relation names, target entities, required relations, and
  /// cardinality for normal and dry-run workflows.
  ///
  /// @param skipDatabaseChecks when true, skips repository lookups for target
  /// existence and target-template compatibility
  /// @param omitMissingOptionalTargets when true, filters repository-missing
  /// identifiers from optional relations and omits relations left empty
  /// @return relations with tolerated missing optional targets filtered out
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
    RelationValidationOptions options = new RelationValidationOptions(skipDatabaseChecks,
        omitMissingOptionalTargets);
    List<Relation> normalizedRelations = normalizeRelations(template.identifier(), relations,
        definitionsByName, targetIdentifiersByName, violations, options);

    validateRelationDefinitions(template.identifier(), definitions, relationsByName,
        targetIdentifiersByName, violations);
    return normalizedRelations;
  }

  /// Filters repository-missing identifiers from optional relations when enabled
  /// and records the remaining identifiers for required and cardinality checks.
  ///
  /// @param templateIdentifier the identifier of the source entity template
  /// @param relations the relations and their target identifiers to validate
  /// @param definitionsByName the template relation definitions indexed by name
  /// @param targetIdentifiersByName receives the identifiers remaining after
  /// filtering
  /// @param violations the accumulator for validation violations
  /// @param options the relation validation options
  /// @return relations with tolerated missing optional targets filtered out
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

  /// Validates one relation and filters repository-missing target identifiers
  /// from optional relations when enabled. Omits the relation if no targets
  /// remain, but retains undefined relations so their name violations are
  /// reported.
  ///
  /// @param templateIdentifier the identifier of the source entity template
  /// @param relation the relation and target identifiers to validate
  /// @param definitionsByName the template relation definitions indexed by name
  /// @param violations the accumulator for validation violations
  /// @param options the relation validation options
  /// @return the filtered relation, if any, and its remaining target identifiers
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
    boolean omitUnresolvedTargets = options.omitMissingOptionalTargets() && definition != null
        && !definition.required();
    List<String> normalizedTargetIdentifiers = normalizeTargetIdentifiers(relation,
        targetIdentifiers, violations, options.skipDatabaseChecks(), omitUnresolvedTargets);

    if (omitUnresolvedTargets && normalizedTargetIdentifiers.isEmpty()) {
      return new RelationNormalization(Optional.empty(), normalizedTargetIdentifiers);
    }

    Relation normalizedRelation = new Relation(relation.id(), relation.name(),
        relation.targetTemplateIdentifier(), normalizedTargetIdentifiers);
    return new RelationNormalization(Optional.of(normalizedRelation), normalizedTargetIdentifiers);
  }

  /// Looks up target identifiers, reports missing targets unless optional
  /// filtering is enabled, and checks the templates of found entities. In
  /// optional filtering mode, removes identifiers that have no repository match.
  ///
  /// @param relation the relation whose targets are checked
  /// @param targetIdentifiers the non-blank target identifiers to validate
  /// @param violations the accumulator for validation violations
  /// @param skipDatabaseChecks whether to skip the repository lookup and target
  /// template checks
  /// @param omitUnresolvedTargets whether repository-missing identifiers should
  /// be filtered without adding missing-target violations
  /// @return target identifiers remaining after optional filtering
  private List<String> normalizeTargetIdentifiers(Relation relation, List<String> targetIdentifiers,
      Violations violations, boolean skipDatabaseChecks, boolean omitUnresolvedTargets) {
    if (skipDatabaseChecks || targetIdentifiers.isEmpty()) {
      return targetIdentifiers;
    }

    List<EntitySummary> existingEntities = entityRepository.findByIdentifierIn(targetIdentifiers);
    Set<String> existingIdentifiers = existingEntities.stream().map(EntitySummary::identifier)
        .collect(Collectors.toSet());

    validateTargetExistence(relation, targetIdentifiers, existingIdentifiers, violations,
        omitUnresolvedTargets);
    validateRelationTargetTemplates(relation, existingEntities, violations);

    if (omitUnresolvedTargets) {
      return targetIdentifiers.stream().filter(existingIdentifiers::contains).toList();
    }

    return targetIdentifiers;
  }

  /// Adds a missing-target violation for each identifier absent from the
  /// repository unless optional filtering is enabled.
  ///
  /// @param relation the relation being validated
  /// @param targetIdentifiers the non-blank target identifiers to check
  /// @param existingIdentifiers the identifiers found in the repository
  /// @param violations the accumulator for validation violations
  /// @param omitUnresolvedTargets whether repository-missing identifiers should
  /// be tolerated without a violation
  private void validateTargetExistence(Relation relation, List<String> targetIdentifiers,
      Set<String> existingIdentifiers, Violations violations, boolean omitUnresolvedTargets) {
    for (String identifier : targetIdentifiers) {
      if (!existingIdentifiers.contains(identifier) && !omitUnresolvedTargets) {
        violations.add(RELATION_TARGET_ENTITY_NOT_FOUND, relation.name(), identifier);
      }
    }
  }

  /// Reports missing required relations and multiple targets on single-target
  /// relations using identifiers remaining after optional filtering.
  ///
  /// @param templateIdentifier the identifier of the source entity template
  /// @param definitions the relation definitions to validate
  /// @param relationsByName the provided relations indexed by name
  /// @param targetIdentifiersByName the target identifiers remaining after
  /// optional filtering, indexed by relation name
  /// @param violations the accumulator for validation violations
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

  /// Validates entity relations for dry-run executions.
  ///
  /// Keeps structural validation (relation names, required relations, and
  /// cardinality) while skipping repository-backed target existence and template
  /// compatibility checks.
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
