package com.decathlon.idp_core.domain.model.entity_mapping;

import java.util.Map;
import java.util.UUID;

import com.decathlon.idp_core.domain.exception.entity_mapping.EntityDynamicMappingConfigurationException;

/// Domain model representing dynamic entity mapping configuration.
///
/// Each mapping defines how to transform inbound webhook events into entity instances,
/// including property/relation mappings and JSLT transformation rules.
///
/// Note: The technical ID is managed purely at the infrastructure layer
/// (persisted in entity_dynamic_mapping table) and is NOT part of the domain model.
public record EntityDynamicMapping(UUID id, String identifier, String templateIdentifier,
    String filter, String entityIdentifier, String entityTitle, Map<String, String> properties,
    Map<String, String> relations) {

  public EntityDynamicMapping {
    if (isBlank(identifier)) {
      throw new EntityDynamicMappingConfigurationException(
          "Entity dynamic mapping identifier cannot be empty");
    }
    if (isBlank(templateIdentifier)) {
      throw new EntityDynamicMappingConfigurationException("Template identifier cannot be empty");
    }

    if (isBlank(filter)) {
      throw new EntityDynamicMappingConfigurationException("Filter cannot be empty");
    }

    if (isBlank(entityIdentifier)) {
      throw new EntityDynamicMappingConfigurationException("EntityIdentifier cannot be empty");
    }

    if (isBlank(entityTitle)) {
      throw new EntityDynamicMappingConfigurationException("EntityTitle cannot be empty");
    }

    properties = properties == null ? Map.of() : Map.copyOf(properties);
    relations = relations == null ? Map.of() : Map.copyOf(relations);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
