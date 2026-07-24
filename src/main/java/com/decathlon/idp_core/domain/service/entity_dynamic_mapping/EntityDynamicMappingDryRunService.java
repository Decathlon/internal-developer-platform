package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.ExpressionEvaluationFailedException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult.DryRunEntityResult;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.ErrorType;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityValidationService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntityDynamicMappingDryRunService {

  private final MappingEnginePort mappingEnginePort;
  private final EntityTemplateService entityTemplateService;
  private final EntityDynamicMappingValidationService entityDynamicMappingValidationService;
  private final EntityValidationService entityValidationService;

  /**
   * Executes dry-run validation for one mapping definition and payload sample.
   * Returns success, skipped, and failure entries as a {@link DryRunResult}.
   */
  @Transactional(readOnly = true)
  public DryRunResult executeSingleMappingDryRun(EntityDynamicMapping mapping, String rawPayload) {
    entityDynamicMappingValidationService.validateMapping(mapping);

    List<DryRunEntityResult> results = processMapping(mapping, rawPayload);
    return new DryRunResult(results);
  }

  /**
   * Processes one mapping against a payload and returns per-entity dry-run
   * results.
   */
  public List<DryRunEntityResult> processMapping(EntityDynamicMapping mapping, String rawPayload) {
    String templateIdentifier = mapping.entityTemplateIdentifier();

    try {
      return mapAndValidateEntity(mapping, rawPayload, templateIdentifier);
    } catch (EntityDynamicMappingJsltErrorException
        | EntityDynamicMappingConfigurationException e) {
      throw e;
    } catch (ExpressionEvaluationFailedException e) {
      throw new EntityDynamicMappingJsltErrorException(e.getMessage());
    } catch (Exception e) {
      return List.of(DryRunEntityResult.failure(templateIdentifier, ErrorType.JSLT_ERROR,
          "Unexpected transformation error: " + e.getMessage()));
    }
  }

  /**
   * Maps payload to one entity, then validates that mapped entity against the
   * target template.
   */
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

  /**
   * Validates one mapped entity and builds the corresponding dry-run result
   * entry.
   */
  private DryRunEntityResult validateAndBuildResult(Entity entity, EntityTemplate template,
      String templateIdentifier) {
    try {
      entityValidationService.validateForDryRun(entity, template);
      return DryRunEntityResult.success(templateIdentifier, entity);
    } catch (EntityValidationException validationException) {
      throw new EntityDynamicMappingJsltErrorException(
          "Entity validation failed: " + validationException.getMessage());
    }
  }

  /**
   * Enriches extracted relations with target template identifiers coming from the
   * template definition. Leaves unknown relation names unchanged so downstream
   * validation can report them explicitly.
   */
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
