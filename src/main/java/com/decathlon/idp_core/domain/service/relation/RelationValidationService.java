package com.decathlon.idp_core.domain.service.relation;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NOT_DEFINED_IN_TEMPLATE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_ENTITY_NOT_FOUND;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TOO_MANY_TARGETS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity.Violations;

import lombok.RequiredArgsConstructor;

/// Domain service validating entity relations against template relation definitions.
@Service
@RequiredArgsConstructor
public class RelationValidationService {

  private final EntityRepositoryPort entityRepository;

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
  /// in the database.
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
