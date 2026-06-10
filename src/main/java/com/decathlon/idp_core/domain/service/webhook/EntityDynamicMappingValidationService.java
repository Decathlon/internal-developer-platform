package com.decathlon.idp_core.domain.service.webhook;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.webhook.WebhookTemplateHasNoPropertiesException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

import lombok.RequiredArgsConstructor;

/// Validates webhook dynamic mappings against their target entity template.
/// This service ensures the mapping references an existing template, that all
/// mapped properties and relations exist in that template, and that required
/// template elements are provided before the mapping is accepted.
@Service
@Validated
@RequiredArgsConstructor
public class EntityDynamicMappingValidationService {
  private final EntityTemplateService entityTemplateService;
  private final EntityDynamicMapperValidator entityDynamicMapperValidator;
  private final EntityTemplateValidationService entityTemplateValidationService;

  /// Validates all mappings attached to a webhook connector.
  ///
  /// @param mappings the mappings to validate
  /// @throws WebhookTemplateHasNoPropertiesException when one or more mappings
  /// are invalid
  public void validateWebhookMapping(List<EntityDynamicMapping> mappings) {
    mappings.forEach(this::validateMapping);
  }

  /// Validates a single [EntityDynamicMapping]:
  /// - The referenced EntityTemplate must exist.
  /// - Each key in `properties` must match a property defined in the template.
  /// - Required properties and relations from the target template are present.
  /// - The mapping expression syntax is valid.
  ///
  /// @param webhookMapping the mapping to validate
  private void validateMapping(EntityDynamicMapping webhookMapping) {
    String templateIdentifier = webhookMapping.templateIdentifier();
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);
    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);
    entityTemplateValidationService.validatePropertiesExistInTemplate(webhookMapping.properties(),
        entityTemplate.propertiesDefinitions());
    validateRequiredPropertiesAreMapped(webhookMapping.properties(),
        entityTemplate.propertiesDefinitions());
    entityTemplateValidationService
        .validateRelationNameAlreadyExistInTemplate(webhookMapping.relations(), entityTemplate);
    validateRequiredRelationDefinitionsAreMapped(webhookMapping.relations(),
        entityTemplate.relationsDefinitions());
    entityDynamicMapperValidator.validate(webhookMapping);
  }

  /// Validates that all required relation definitions in the target template
  /// are provided by the mapping.
  ///
  /// @param mappingRelations relations declared by the mapping
  /// @param templateRelations relation definitions declared by the template
  /// @throws WebhookTemplateHasNoPropertiesException when one or more required
  /// relations are missing in the mapping
  private void validateRequiredRelationDefinitionsAreMapped(Map<String, String> mappingRelations,
      List<RelationDefinition> templateRelations) {
    List<String> missingRelations = templateRelations.stream().filter(RelationDefinition::required)
        .map(RelationDefinition::name).filter(requiredRelation -> mappingRelations == null
            || !mappingRelations.containsKey(requiredRelation))
        .toList();
    if (!missingRelations.isEmpty()) {
      throw new WebhookTemplateHasNoPropertiesException(
          String.format("The mapping is missing required template relations: %s",
              String.join(", ", missingRelations)));
    }
  }

  /// Validates that all required property definitions in the target template
  /// are provided by the mapping.
  ///
  /// @param mappingProperties properties declared by the mapping
  /// @param templateProperties property definitions declared by the template
  /// @throws WebhookTemplateHasNoPropertiesException when one or more required
  /// properties are missing in the mapping
  private void validateRequiredPropertiesAreMapped(Map<String, String> mappingProperties,
      List<PropertyDefinition> templateProperties) {
    List<String> missingProperties = templateProperties.stream()
        .filter(PropertyDefinition::required).map(PropertyDefinition::name)
        .filter(requiredName -> !mappingProperties.containsKey(requiredName)).toList();

    if (!missingProperties.isEmpty()) {
      throw new WebhookTemplateHasNoPropertiesException(
          String.format("The mapping is missing required template properties: %s",
              String.join(", ", missingProperties)));
    }

  }
}
