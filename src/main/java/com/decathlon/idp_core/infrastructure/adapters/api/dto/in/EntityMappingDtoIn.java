package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/// Entity projection section for an inbound webhook mapping.
public record EntityMappingDtoIn(
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_ENTITY_IDENTIFIER_MANDATORY) String identifier,
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_ENTITY_NAME_MANDATORY) String name,
    @NotNull(message = ENTITY_DYNAMIC_MAPPING_ENTITY_PROPERTIES_MANDATORY) Map<String, String> properties,
    @NotNull(message = ENTITY_DYNAMIC_MAPPING_ENTITY_RELATIONS_MANDATORY) @JsonDeserialize(using = RelationMappingsDeserializer.class) @Valid List<RelationMappingDtoIn> relations) {

  public EntityMappingDtoIn {
    properties = properties != null ? Map.copyOf(properties) : null;
    relations = relations != null ? List.copyOf(relations) : null;
  }
}
