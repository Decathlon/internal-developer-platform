package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Entity projection section for an inbound webhook mapping.
public record EntityMappingDtoIn(
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_ENTITY_IDENTIFIER_MANDATORY) String identifier,
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_ENTITY_NAME_MANDATORY) String name,
    @NotNull(message = ENTITY_DYNAMIC_MAPPING_ENTITY_PROPERTIES_MANDATORY) Map<String, String> properties,
    @NotNull(message = ENTITY_DYNAMIC_MAPPING_ENTITY_RELATIONS_MANDATORY) Map<String, String> relations) {

  public EntityMappingDtoIn {
    properties = properties != null ? Map.copyOf(properties) : null;
    relations = relations != null ? Map.copyOf(relations) : null;
  }
}
