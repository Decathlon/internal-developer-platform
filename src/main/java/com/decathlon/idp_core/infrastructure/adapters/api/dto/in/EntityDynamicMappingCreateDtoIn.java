package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/// Mapping rule request for inbound webhook transformation.
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EntityDynamicMappingCreateDtoIn(

    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_IDENTIFIER_MANDATORY) String identifier,
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_TEMPLATE_IDENTIFIER_MANDATORY) String entityTemplateIdentifier,
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_FILTER_MANDATORY) String filter,
    @NotBlank(message = ENTITY_DYNAMIC_MAPPING_NAME_MANDATORY) String name, String description,
    @NotNull(message = ENTITY_DYNAMIC_MAPPING_ENTITY_MANDATORY) @Valid EntityMappingDtoIn entity) {

  /// Returns a CommonFields view for compatibility with the mapper.
  public EntityDynamicMappingDtoInCommonFields commonFields() {
    return new EntityDynamicMappingDtoInCommonFields(entityTemplateIdentifier, filter, name,
        description, entity);
  }
}
