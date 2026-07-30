package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/// Entity projection section for an inbound webhook mapping.
public record EntityMappingDtoIn(
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_ENTITY_IDENTIFIER_MANDATORY) String identifier,
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_ENTITY_NAME_MANDATORY) String name,
    Map<String, String> properties, @Valid List<EntityDynamicMappingRelationDtoIn> relations) {

  public EntityMappingDtoIn {
    properties = properties != null ? Map.copyOf(properties) : Map.of();
    relations = relations != null ? List.copyOf(relations) : List.of();
  }
}
