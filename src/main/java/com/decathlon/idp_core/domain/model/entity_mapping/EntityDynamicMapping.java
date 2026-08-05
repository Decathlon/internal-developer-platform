package com.decathlon.idp_core.domain.model.entity_mapping;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;

/// Domain model representing dynamic entity mapping configuration.
///
/// Each mapping defines how to transform inbound webhook events into entity instances,
/// including property/relation mappings and JSLT transformation rules.
///
/// Note: The technical ID is managed purely at the infrastructure layer
/// (persisted in entity_dynamic_mapping table) and is NOT part of the domain model.
public record EntityDynamicMapping(UUID id, String identifier, String entityTemplateIdentifier,
    String filter, String name, String description, String entityIdentifier, String entityName,
    Map<String, String> properties, List<RelationMapping> relations) {

  public EntityDynamicMapping {
    if (isBlank(identifier)) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_IDENTIFIER_MANDATORY);
    }
    if (isBlank(name)) {
      throw new EntityDynamicMappingConfigurationException(ENTITY_DYNAMIC_MAPPING_NAME_MANDATORY);
    }
    if (isBlank(entityTemplateIdentifier)) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_TEMPLATE_IDENTIFIER_MANDATORY);
    }

    if (isBlank(filter)) {
      throw new EntityDynamicMappingConfigurationException(ENTITY_DYNAMIC_MAPPING_FILTER_MANDATORY);
    }

    if (isBlank(entityIdentifier)) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_ENTITY_IDENTIFIER_MANDATORY);
    }

    if (isBlank(entityName)) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_ENTITY_NAME_MANDATORY);
    }

    properties = properties == null ? Map.of() : Map.copyOf(properties);
    relations = relations == null ? List.of() : List.copyOf(relations);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
