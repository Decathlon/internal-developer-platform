package com.decathlon.idp_core.domain.service.webhook;

import com.decathlon.idp_core.domain.exception.webhook.WebhookTemplateHasNoPropertiesException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Service
@Validated
@RequiredArgsConstructor
public class EntityDynamicMappingValidationService {
    private final EntityTemplateService entityTemplateService;
    private final EntityDynamicMapperValidator entityDynamicMapperValidator;
    private final EntityTemplateValidationService entityTemplateValidationService;

    /**
     * Validates all mappings of a WebhookConnector.
     *
     * @param mappings the list of {@link EntityDynamicMapping} to validate
     */
    public void validateWebhookMapping(List<EntityDynamicMapping> mappings) {
        mappings.forEach(this::validateMapping);
    }

    /**
     * Validates a single {@link EntityDynamicMapping}:
     * <ul>
     *   <li>The referenced EntityTemplate must exist.</li>
     *   <li>Each key in {@code properties} must match a property defined in the template.</li>
     * </ul>
     *
     * @param webhookMapping the mapping to validate
     */
    private void validateMapping(EntityDynamicMapping webhookMapping) {
        String templateIdentifier = webhookMapping.templateIdentifier();
        entityTemplateValidationService.validateTemplateExists(templateIdentifier);
        EntityTemplate entityTemplate = entityTemplateService.getEntityTemplateByIdentifier(templateIdentifier);
        validatePropertiesExistInTemplate(webhookMapping.properties(), entityTemplate.propertiesDefinitions());
        validateRequiredPropertiesAreMapped(webhookMapping.properties(), entityTemplate.propertiesDefinitions());
        validateRelationNameAlreadyExistInTemplate(webhookMapping.relations(), entityTemplate);
        validateRequiredRelationDefinitionsAreMapped(webhookMapping.relations(), entityTemplate.relationsDefinitions());
        entityDynamicMapperValidator.validate(webhookMapping);
    }

    private void validateRequiredRelationDefinitionsAreMapped(Map<String, String> mappingRelations, List<RelationDefinition> templateRelations) {
        List<String> missingRelations = templateRelations.stream()
                .filter(RelationDefinition::required)
                .map(RelationDefinition::name)
                .filter(requiredRelation -> mappingRelations == null || !mappingRelations.containsKey(requiredRelation))
                .toList();
        if (!missingRelations.isEmpty()) {
            throw new WebhookTemplateHasNoPropertiesException(String.format("The mapping is missing required template relations: %s", String.join(", ", missingRelations)));
        }
    }

    private void validateRequiredPropertiesAreMapped(Map<String, String> mappingProperties, List<PropertyDefinition> templateProperties) {
        List<String> missingProperties = templateProperties.stream()
                .filter(PropertyDefinition::required)
                .map(PropertyDefinition::name)
                .filter(requiredName -> !mappingProperties.containsKey(requiredName))
                .toList();

        if (!missingProperties.isEmpty()) {
            throw new WebhookTemplateHasNoPropertiesException(String.format("The mapping is missing required template properties: %s", String.join(", ", missingProperties)));
        }

    }

    private void validatePropertiesExistInTemplate(Map<String, String> mappingProperties, List<PropertyDefinition> templateProperties) {

        if (!mappingProperties.isEmpty() && templateProperties.isEmpty()) {
            throw new WebhookTemplateHasNoPropertiesException("The mapping defines properties but the target template has no property definitions");
        }

        mappingProperties.keySet().forEach(propertyName ->
                entityTemplateValidationService.validatePropertyNameAlreadyExistInTemplate(templateProperties, propertyName)
        );
    }

    private void validateRelationNameAlreadyExistInTemplate(Map<String, String> webhookMappingRelations, EntityTemplate entityTemplate) {
        if (webhookMappingRelations == null || webhookMappingRelations.isEmpty()) {
            return;
        }
        webhookMappingRelations.keySet().forEach(relationName -> entityTemplateValidationService.validateRelationNameAlreadyExistInTemplate(entityTemplate.relationsDefinitions(), relationName));
    }
}
