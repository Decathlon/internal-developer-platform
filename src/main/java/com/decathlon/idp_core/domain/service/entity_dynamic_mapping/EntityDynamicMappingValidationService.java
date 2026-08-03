package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;
import com.decathlon.idp_core.domain.service.relation.RelationValidationService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class EntityDynamicMappingValidationService {

  private final EntityTemplateService entityTemplateService;
  private final EntityDynamicMapperValidator entityDynamicMapperValidator;
  private final PropertyValidationService propertyValidationService;
  private final RelationValidationService relationValidationService;

  public void validateMappings(List<EntityDynamicMapping> mappings) {
    mappings.forEach(this::validateMapping);
  }

  public void validateMapping(EntityDynamicMapping entityDynamicMapping) {
    String templateIdentifier = entityDynamicMapping.entityTemplateIdentifier();
    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);

    Map<String, String> properties = entityDynamicMapping.properties() != null
        ? entityDynamicMapping.properties()
        : Collections.emptyMap();
    Map<String, String> relations = entityDynamicMapping.relations() != null
        ? entityDynamicMapping.relations()
        : Collections.emptyMap();

    // Validate properties against template
    propertyValidationService.validateMappingPropertiesAgainstTemplate(entityTemplate,
        List.copyOf(properties.keySet()));

    // Validate relations against template
    relationValidationService.validateMappingRelationsAgainstTemplate(entityTemplate,
        List.copyOf(relations.keySet()));

    entityDynamicMapperValidator.validate(entityDynamicMapping);
  }
}
