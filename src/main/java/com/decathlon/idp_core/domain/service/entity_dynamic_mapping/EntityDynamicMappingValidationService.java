package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
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

    List<String> propertyNames = entityDynamicMapping.properties() != null
        ? List.copyOf(entityDynamicMapping.properties().keySet())
        : List.of();

    // Extract relation names from the ordered relation list for template
    // validation.
    List<String> relationNames = entityDynamicMapping.relations() != null
        ? entityDynamicMapping.relations().stream().map(RelationMapping::name).toList()
        : List.of();

    // Validate properties against template
    propertyValidationService.validateMappingPropertiesAgainstTemplate(entityTemplate,
        propertyNames);

    // Validate relations against template
    relationValidationService.validateMappingRelationsAgainstTemplate(entityTemplate,
        relationNames);

    entityDynamicMapperValidator.validate(entityDynamicMapping);
  }
}
