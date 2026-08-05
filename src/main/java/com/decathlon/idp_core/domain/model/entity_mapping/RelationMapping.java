package com.decathlon.idp_core.domain.model.entity_mapping;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.util.List;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;

public record RelationMapping(String name, List<String> targetIdentifiersExpressions) {

  public RelationMapping {
    if (name == null || name.isBlank()) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_ENTITY_RELATION_NAME_MANDATORY);
    }
    if (targetIdentifiersExpressions == null || targetIdentifiersExpressions.isEmpty()) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_ENTITY_RELATION_EXPRESSIONS_LIST_MESSAGE);
    }
    if (targetIdentifiersExpressions.stream()
        .anyMatch(expression -> expression == null || expression.isBlank())) {
      throw new EntityDynamicMappingConfigurationException(
          ENTITY_DYNAMIC_MAPPING_ENTITY_RELATION_EXPRESSIONS_LIST_MESSAGE);
    }
    targetIdentifiersExpressions = List.copyOf(targetIdentifiersExpressions);
  }
}
