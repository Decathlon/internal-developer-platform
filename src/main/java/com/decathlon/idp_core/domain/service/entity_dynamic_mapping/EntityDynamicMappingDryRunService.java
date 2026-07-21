package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult.DryRunEntityResult;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
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

  /// Executes dry-run validation for one mapping definition and payload sample.
  /// Returns success, skipped, and failure entries as a DryRunResult.
  @Transactional(readOnly = true)
  public DryRunResult executeSingleMappingDryRun(EntityDynamicMapping mapping, String rawPayload) {
    entityDynamicMappingValidationService.validateMapping(mapping);

    List<DryRunEntityResult> results = processMapping(mapping, rawPayload);
    return new DryRunResult(results);
  }

  /// Processes one mapping against a payload and returns per-entity dry-run
  /// results.
  public List<DryRunEntityResult> processMapping(EntityDynamicMapping mapping, String rawPayload) {
    String templateIdentifier = mapping.entityTemplateIdentifier();

    try {
      return mapAndValidateEntities(mapping, rawPayload, templateIdentifier);

    } catch (EntityDynamicMappingConfigurationException e) {
      return List
          .of(DryRunEntityResult.failure(templateIdentifier, ErrorType.JSLT_ERROR, e.getMessage()));
    } catch (Exception e) {
      return List.of(DryRunEntityResult.failure(templateIdentifier, ErrorType.JSLT_ERROR,
          "Unexpected transformation error: " + e.getMessage()));
    }
  }

  /// Maps payload to entities, then validates each mapped entity against target
  /// template.
  private List<DryRunEntityResult> mapAndValidateEntities(EntityDynamicMapping mapping,
      String rawPayload, String templateIdentifier) {
    List<Entity> mappedEntities = mappingEnginePort.mapToEntities(rawPayload, mapping);

    if (mappedEntities == null || mappedEntities.isEmpty()) {
      return List.of(DryRunEntityResult.skipped(templateIdentifier));
    }

    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);

    return mappedEntities.stream()
        .map(entity -> validateAndBuildResult(entity, template, templateIdentifier)).toList();
  }

  /// Validates one mapped entity and builds the corresponding dry-run result
  /// entry.
  private DryRunEntityResult validateAndBuildResult(Entity entity, EntityTemplate template,
      String templateIdentifier) {
    try {
      entityValidationService.validateForCreation(entity, template);
      return DryRunEntityResult.success(templateIdentifier, entity);
    } catch (Exception validationException) {
      return DryRunEntityResult.failure(templateIdentifier, ErrorType.JSLT_ERROR,
          "Entity validation failed: " + validationException.getMessage());
    }
  }
}
