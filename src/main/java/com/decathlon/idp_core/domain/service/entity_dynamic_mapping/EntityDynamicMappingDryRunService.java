package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.ExpressionEvaluationFailedException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult.DryRunEntityResult;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.ErrorType;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.Violations;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;
import com.decathlon.idp_core.domain.service.relation.RelationValidationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntityDynamicMappingDryRunService {

  private final MappingEnginePort mappingEnginePort;
  private final EntityTemplateService entityTemplateService;
  private final EntityDynamicMappingValidationService entityDynamicMappingValidationService;
  private final PropertyValidationService propertyValidationService;
  private final RelationValidationService relationValidationService;

  /// Executes dry-run validation for one mapping definition and payload sample.
  ///
  /// Returns success, skipped, and failure entries as a `DryRunResult`.
  @Transactional(readOnly = true)
  public DryRunResult executeDryRun(EntityDynamicMapping mapping, String rawPayload) {
    entityDynamicMappingValidationService.validateMapping(mapping);

    List<DryRunEntityResult> results = processMapping(mapping, rawPayload);
    return new DryRunResult(results);
  }

  /// Processes one mapping against a payload and returns per-entity dry-run
  /// results.
  ///
  /// `ExpressionEvaluationFailedException` is re-thrown as-is so that the
  /// `ApiExceptionHandler` dedicated handler can log expression/reason context
  /// with full diagnostic detail (HTTP 422). Wrapping it into
  /// `EntityDynamicMappingJsltErrorException` would lose that information.
  public List<DryRunEntityResult> processMapping(EntityDynamicMapping mapping, String rawPayload) {
    String templateIdentifier = mapping.entityTemplateIdentifier();

    try {
      return mapAndValidateEntity(mapping, rawPayload, templateIdentifier);
    } catch (EntityDynamicMappingJsltErrorException | EntityDynamicMappingConfigurationException
        | ExpressionEvaluationFailedException e) {
      throw e;
    } catch (Exception e) {
      return List.of(DryRunEntityResult.failure(templateIdentifier, ErrorType.JSLT_ERROR,
          "Unexpected transformation error: " + e.getMessage()));
    }
  }

  /// Maps payload to one entity, then validates that mapped entity against the
  /// target template.
  private List<DryRunEntityResult> mapAndValidateEntity(EntityDynamicMapping mapping,
      String rawPayload, String templateIdentifier) {
    Entity mappedEntity = mappingEnginePort.mapToEntity(rawPayload, mapping);

    if (mappedEntity == null) {
      return List.of(DryRunEntityResult.skipped(templateIdentifier));
    }

    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);
    Entity enrichedEntity = enrichRelationsWithTargetTemplates(mappedEntity, template);

    return List.of(validateAndBuildResult(enrichedEntity, template, templateIdentifier));
  }

  /// Validates one mapped entity and builds the corresponding dry-run result
  /// entry.
  private DryRunEntityResult validateAndBuildResult(Entity entity, EntityTemplate template,
      String templateIdentifier) {
    try {
      validateForDryRun(entity, template);
      return DryRunEntityResult.success(templateIdentifier, entity);
    } catch (EntityValidationException validationException) {
      throw new EntityDynamicMappingJsltErrorException(
          "Entity validation failed: " + validationException.getMessage());
    }
  }

  /// Validates entity data for dry-run execution.
  ///
  /// **Contract:** dry-run verifies that the mapped entity structurally conforms
  /// to the template without requiring database persistence preconditions.
  /// It therefore skips uniqueness checks and relation target-entity existence
  /// checks, while still validating required properties, relation names, and
  /// relation cardinality.
  ///
  /// @param entity the mapped entity payload to validate
  /// @param template the already-resolved template the entity must conform to
  /// @throws EntityValidationException when one or more validation rules are
  /// violated
  private void validateForDryRun(Entity entity, EntityTemplate template) {
    Violations violations = new Violations();

    List<PropertyDefinition> definitions = Optional.ofNullable(template.propertiesDefinitions())
        .orElse(List.of());

    Map<String, Property> propertiesByName = Optional.ofNullable(entity.properties())
        .orElse(List.of()).stream().filter(p -> p.name() != null)
        .collect(Collectors.toMap(Property::name, p -> p, (left, _) -> left));

    propertyValidationService.validatePropertiesAgainstTemplate(template, definitions,
        propertiesByName, violations);

    relationValidationService.validateRelationsAgainstTemplateForDryRun(template,
        entity.relations(), violations);

    if (!violations.isEmpty()) {
      throw new EntityValidationException(violations.asList());
    }
  }

  /// Enriches extracted relations with target template identifiers coming from
  /// the template definition.
  ///
  /// Leaves unknown relation names unchanged so downstream validation can report
  /// them explicitly.
  private Entity enrichRelationsWithTargetTemplates(Entity entity, EntityTemplate template) {
    List<RelationDefinition> relationDefinitions = template.relationsDefinitions() != null
        ? template.relationsDefinitions()
        : List.of();

    List<Relation> enrichedRelations = entity.relations().stream()
        .map(relation -> relationDefinitions.stream()
            .filter(definition -> definition.name().equals(relation.name())).findFirst()
            .map(definition -> new Relation(relation.id(), relation.name(),
                definition.targetTemplateIdentifier(), relation.targetEntityIdentifiers()))
            .orElse(relation))
        .toList();

    return new Entity(entity.id(), entity.templateIdentifier(), entity.name(), entity.identifier(),
        entity.properties(), enrichedRelations);
  }
}
